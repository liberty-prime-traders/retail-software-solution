# RTSS — Kafka Messaging Patterns

Read this before adding a new event, producer, processor, consumer group, or any retry/DLT logic. See also `messaging/kafka/README.md` for why `transaction/` and `catalog/` are two separate pipelines and when to use each.

---

## Topics and consumer groups

All constants live in `KafkaConstants` (`messaging/kafka/common/KafkaConstants.kt`).

| Topic                            | Consumer groups                       | Purpose                                                                |
|----------------------------------|---------------------------------------|------------------------------------------------------------------------|
| `transaction-events`             | `inventory-group`, `accounting-group` | Location-scoped transactional events (sales, deliveries, payments)     |
| `catalog-events`                 | `catalog-sync-group`                  | Org-scoped catalog changes that must propagate to all locations        |
| `notifications`                  | `notification-alerts-group`           | Internal failure alerts from consumers                                 |
| `transaction-events.<group>.DLT` | —                                     | Per-consumer-group dead-letter topic; created automatically at startup |

Topics are declared in `TopicConfig` (6 partitions, 1 replica). All new topics go there **and** in `KafkaConstants`.

---

## Publishing events

### TransactionEvent (location-scoped)

Use `ApplicationEventPublisher` — never call `KafkaTemplate` directly from business code.

```kotlin
eventPublisher.publishEvent(
    SaleConfirmedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = EventSourceContext.LocationLevel(
            orgSchema = SessionContextProvider.getOrganizationSchema(),
            locationSchema = SessionContextProvider.getLocationSchema()
        ),
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = sale.id!!,
        // ... event-specific fields
    )
)
```

`TransactionEventProducer` listens with `@TransactionalEventListener(phase = AFTER_COMMIT)` — the event only reaches Kafka if the enclosing transaction commits. Rolled-back transactions never emit.

Partition key is derived from `event.sourceContext`: `locationSchema` for `LocationLevel` events (so events for the same location land on the same partition in order), `orgSchema` for `OrgLevel` events (e.g. `OpeningBalanceUpsertedEvent`) — `OrgLevel` has no `locationSchema` to key on.

### CatalogChangedEvent (org-scoped)

Call `CatalogEventHandler.publish(tableName, entityId)` — it constructs the event and calls `CatalogEventProducer` directly. Catalog events are fire-and-forget (no log, no DLT).

---

## Event types

### BaseEvent (abstract)

```kotlin
abstract val eventId: UUID
abstract val sourceContext: EventSourceContext   // OrgLevel or LocationLevel
abstract val timestamp: Instant
abstract val correlationId: UUID?
```

### TransactionEvent (sealed, extends BaseEvent)

```kotlin
abstract override val sourceContext: EventSourceContext.LocationLevel
abstract val sourceDocumentId: UUID   // ID of the originating document (sale, delivery, …)
```

Add a new `TransactionEvent` subtype as a `data class` in `messaging/kafka/transaction/events/`. Every new subtype **must** have a corresponding `EventReissueHandler` — `TransactionEventCoverageCheck` is a `StartupCheck` that enforces this at boot time.

### EventSourceContext

`OrgLevel(orgSchema)` — for catalog events.  
`LocationLevel(orgSchema, locationSchema)` — for transaction events. The framework calls `EventSessionSetup.initFromEvent()` to restore org + location session context in the consumer before any processor runs.

---

## Processor pattern

Every processor that reacts to a `TransactionEvent` implements `TransactionEventProcessor<EVENT>`:

```kotlin
sealed interface TransactionEventProcessor<EVENT : TransactionEvent> {
    val eventType: KClass<EVENT>
    fun handle(event: EVENT)
    fun shouldProcess(event: EVENT): Boolean
}
```

Use the marker sub-interface that matches the consumer group:

- `InventoryEventProcessor<EVENT>` — consumed by `InventoryEventConsumer` (`inventory-group`)
- `AccountingEventProcessor<EVENT>` (abstract class) — consumed by `AccountingEventConsumer` (`accounting-group`); override `prepareLedgerRequest` or `prepareLedgerRequests`

**Required rules for every processor:**

1. `shouldProcess` is a fast read-only existence check on what the processor writes. Return `false` if the work is already done.
2. Back `shouldProcess` with a **unique DB constraint** on the natural key — the consumer support treats `DataIntegrityViolationException` as a race-lost idempotency signal and marks the log `RACE_LOST` without failing.
3. Wrap `handle` in `@TransactionalOnLocationSchema`; `shouldProcess` may be `readOnly = true`.
4. Never call `EventProcessingLogService` from a processor — the consumer support handles logging around the processor lifecycle.

---

## Consumer support — what the framework does for you

`TransactionEventConsumerSupport.consume()` orchestrates each event delivery:

1. Finds all processors whose `eventType` matches the event class.
2. Filters out processors already completed (`isProcessorCompleted`) and processors where `shouldProcess` returns false.
3. Inserts a `PENDING` log entry, then calls each processor's `handle`.
4. Marks each processor completed in `completedProcessors` after `handle` returns.
5. Marks the log `PROCESSED` when all processors are done.
6. On `DataIntegrityViolationException` → marks `RACE_LOST` (treated as processed, not failed).
7. On any other exception → marks `FAILED`, publishes to DLT, sends a notification event.

You never call this class directly — it is called by `InventoryEventConsumer` and `AccountingEventConsumer`.

---

## EventProcessingLog

**Org-scoped** table (`event_processing_log`, `organizations/business/kafka_log/`) that tracks every `TransactionEvent` delivery per consumer group — regardless of whether the event itself is `OrgLevel` or `LocationLevel`. It lives at org level (not location) because a location-scoped event has no location in session by the time an `OrgLevel` event needs the same log path; putting it under `@TransactionalOnLocationSchema` silently swallowed all `OrgLevel` event logging (`RtsGenericException`/"Location schema not found in session" caught and logged, never surfaced) until `OpeningBalanceUpsertedEvent` exposed it.

Each log entry carries:

- `status`: `PENDING → PROCESSED` (happy path); `FAILED`, `DLT_PUBLISH_FAILED`, `PUBLISH_FAILED`, `RETRYING`
- `resolutionType`: `RACE_LOST`, `DLT_REPLAY`, `REISSUED` (set on manual retry)
- `completedProcessors`: set of processor simple-class-names that have finished for this event+group combination
- `dltPartition` / `dltOffset`: set when the event was forwarded to the DLT, so retry can fetch the exact record
- `sourceLocationId`: nullable, resolved from `event.sourceContext` at insert time (`LocationLevel` → that location's id via `LocationService.getBySchema`; `OrgLevel` → `null`). Mirrors `ledger_entry_group.source_location_id`. This is how a location-scoped retry/reissue later knows which location schema to restore, since the log itself carries no location session.

All `EventProcessingLogService` methods use `@TransactionalOnOrganizationSchema(propagation = Propagation.REQUIRES_NEW)` — the log record persists even when the caller's transaction rolls back.

`EventProcessingLogSweeperJob` sweeps once per org (no per-location loop) — `EventProcessingLogSweepService` operates purely on org-scoped log rows.

---

## Reissue handlers

Every `TransactionEvent` subtype must have exactly one `EventReissueHandler` bean:

```kotlin
interface EventReissueHandler {
    val eventType: KClass<out BaseEvent>
    fun reissue(sourceDocumentId: UUID)
}
```

`reissue` re-reads the source document from the DB and publishes a fresh event via `ApplicationEventPublisher`. It must be `@TransactionalOnLocationSchema(readOnly = true)`.

`TransactionEventCoverageCheck` runs at startup and throws if any `TransactionEvent` subclass has no handler, or if two handlers share the same `eventType.simpleName`.

Naming convention: `<Domain>HandlerForKafka` (e.g. `SaleConfirmedHandlerForKafka`).

---

## Retry / DLT flow

`EventRetryService.retry(logId)` (`@TransactionalOnOrganizationSchema`, in `organizations/business/kafka_log/api/`):
1. If `dltPartition`/`dltOffset` are set, fetches the original event from the DLT and re-publishes to `transaction-events` → marks log `RETRYING`.
2. If no DLT record, finds the `EventReissueHandler` by `eventType` simple name. If `entry.sourceLocationId` is set, resolves that location and calls `SessionContextProvider.initLocation(...)` **before** invoking `reissue` — a location-scoped reissue handler needs location schema in session, and a retry triggered from the endpoint or the sweeper arrives with only org context, not a location already set. Then calls `reissue` → marks log `PROCESSED` with `REISSUED`.

`DltPublisher` is called by consumer support on failure; it captures the session before sending because the Kafka producer callback runs on a non-session thread.

---

## Session setup in consumers

Before any processor runs, `EventSessionSetup.initFromEvent(event)` restores the session:
- `OrgLevel` → sets org context only.
- `LocationLevel` → sets both org and location context.

Consumers run under `ServiceAccountContext.runWithServiceAccount(ServiceAccount.INVENTORY_PROCESSOR / ACCOUNTING_PROCESSOR)` — never the user's session.

---

## Adding a new TransactionEvent — checklist

- [ ] Add `data class MyEvent(...) : TransactionEvent()` in `messaging/kafka/transaction/events/`
- [ ] Add a publisher bean (`XxxHandlerForKafka`) that builds and `publishEvent`s it via `ApplicationEventPublisher`; implement `EventReissueHandler` on the same bean
- [ ] Add one or more `InventoryEventProcessor<MyEvent>` or `AccountingEventProcessor<MyEvent>` implementations with `shouldProcess` backed by a unique DB constraint
- [ ] `TransactionEventCoverageCheck` will fail at startup if the reissue handler is missing — fix before running the app
