# messaging/kafka

Two separate Kafka pipelines live here. They solve different problems and are not interchangeable —
picking the wrong one either loses events or does needless fan-out work.

## `transaction/` — logged, retried, per-consumer-group

For business events where losing one silently is unacceptable: sale confirmation, purchase
delivery, opening balance upserts, and the like.

- Published via `TransactionEventProducer`, an `ApplicationEventPublisher` listener bound to
  `AFTER_COMMIT` — a rolled-back transaction never reaches the broker.
- Every event is logged in `event_processing_log` (`EventProcessingLogService`) before dispatch,
  and every `TransactionEventProcessor` implements `shouldProcess` backed by a unique DB constraint
  on the natural key, so retries — manual (`EventRetryService`, `secured/kafka-event-log/{id}/retry`)
  or swept (`EventProcessingLogSweeperJob`, every 5 minutes) — are idempotent.
- Dispatch and logging both key off `event.sourceContext`: `EventSourceContext.OrgLevel` for
  org-scoped events (e.g. `OpeningBalanceUpsertedEvent`), `EventSourceContext.LocationLevel` for
  location-scoped ones. `event_processing_log` itself lives in the **org** schema regardless —
  its `source_location_id` column records which location a `LocationLevel` event came from
  (`null` for `OrgLevel`), mirroring `ledger_entry_group.source_location_id`.
- Failures publish to a per-consumer-group DLT (`DltPublisher`); a failure that can't even be
  logged is swallowed with an ERROR log line — there is no second safety net, so any new
  `OrgLevel` event needs an explicit test proving it reaches the log, not just a smoke check.

Use this pipeline when a consumer must process an event exactly once, retries matter, and you need
an audit trail of what was processed, when, and by whom.

## `catalog/` — fire-and-forget, org-scoped, fans out to every location

For catalog changes (product, pricing, etc.) that every location's read model needs to pick up.

- Published via `CatalogEventProducer`/`CatalogEventHandler.publish` — no `AFTER_COMMIT` binding,
  no `event_processing_log` entry, no DLT, no retry.
- `CatalogEventHandler.consume` loops `locationService.getAllLocationDtos()` and re-runs the sync
  for every location in the org, unconditionally, on every event.
- There's no natural-key uniqueness guard here, so a redelivered event just re-syncs — cheap and
  safe by construction, not because anything tracks whether it already ran.

Use this pipeline when the operation is idempotent by nature (a sync/upsert against current state,
not an incremental step) and losing an occasional delivery is recoverable by the next change to the
same entity — never for anything that debits, credits, or otherwise mutates state incrementally.
