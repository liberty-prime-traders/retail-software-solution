# stock_transfer

Handles the full lifecycle of inter-location stock transfers: draft → dispatched → received.

## Schemas involved

A stock transfer spans **two** location schemas and **one** org schema:

| Schema                      | What lives there                                                                                                       |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------|
| Source location schema      | `stock_transfer_dispatch`, `stock_transfer_dispatch_line`, `stock_transfer_draft_line`, stock entries, stock movements |
| Destination location schema | `stock_transfer_receipt`, `stock_transfer_receipt_line`, destination stock entries and movements                       |
| Org schema                  | `stock_transfer_order` — the canonical status record visible to both locations                                         |

Because each schema requires its own connection, every cross-schema read or write uses `locationService.withLocationSchema(schema) { ... }` to switch the search path, which requires `REQUIRES_NEW` (a fresh connection) on the gateway method called inside. `withLocationSchema` re-resolves the full location (id, schema, timezone) for the target schema via `LocationService.getBySchema` — it never carries over the caller's own location id.

## Response assembly after a write

`StockTransferResponseAssembler.build` is read-only and always re-reads both schemas via `REQUIRES_NEW`. It must never be called right after a write to either schema in the *same* still-open transaction — that write won't be visible yet to a `REQUIRES_NEW` connection. Every service that mutates its own ambient schema and then needs a response uses a dedicated assembler method instead, passing in the entity/lines it just touched directly: `buildDispatchOnly` (draft line add/update/remove, dispatch creation, and the draft-to-dispatch transition — all source-schema) and `buildDispatchAndReceipt` (receipt line confirm/unconfirm/complete — destination-schema; the dispatch side is still read from the foreign source schema via the gateway, which is safe since nothing was written there in the same transaction).

`ReconciledTransferLineFetcher` (not `StockTransferSchemaGateway`) is what those services call for their own-schema draft/dispatch line reads. It exists specifically so `StockTransferSchemaGateway` can stay uniform: every gateway method is `REQUIRES_NEW` cross-schema access, no exceptions. `ReconciledTransferLineFetcher`'s methods are the opposite — `Propagation.MANDATORY`, meaning they refuse to run without an already-active location-schema transaction and always join it. `StockTransferSchemaGateway.buildDispatchWithLines` itself calls into `ReconciledTransferLineFetcher` (MANDATORY is happy to join the gateway's own REQUIRES_NEW transaction) to avoid duplicating the entity-to-`ReconciledTransferLine` mapping.

## Gateway pattern

`StockTransferSchemaGateway` is a pure data access component for the source location schema. It must not publish Kafka events — event publishing belongs in the service layer, where transaction scope is controlled. This is the rule that prevents the Kafka event from firing before the org-schema write completes.

## Event sequencing rule

All three events (`StockTransferDispatchedEvent`, `StockTransferCancelledEvent`, `StockTransferReceiptCompletedEvent`) must be published **after** the corresponding org-schema status update, within the outer service transaction. They fire on `AFTER_COMMIT` of that transaction. If they were published inside a `REQUIRES_NEW` gateway call, they would fire before the org write — leaving the two schemas inconsistent.

## FIFO allocation

`StockTransferFifoAllocator` splits a draft line into one or more dispatch lines, one per cost group. A single draft line for a product with stock at two different unit costs produces two dispatch lines. This means `recordTransferReceipt` in `StockTransferReceiptStockUpdater` must accumulate a running balance per `locationProductId` across lines — not use a single pre-fetched balance for each line — because multiple lines for the same product share the same balance accumulator.

## Reservation-aware dispatch guard

Before `StockTransferFifoAllocator` runs, `StockTransferDispatchService.dispatch` calls
`StockAvailabilityValidator.guardSufficientStock` (`stock/api/`) with each draft line's
base quantity. This nets on-hand balance against the **total** of any live sale
reservations (`StockReserver.loadReservationBreakdown(...).total`) for the same
`locationProductId`, and throws before allocation if a transfer would take stock a sale
has already reserved. Unlike `SaleValidator.guardSufficientStockForSale`, this call never
excludes anything — draft transfers hold no reservation of their own today, so there is no
"self" to exclude. If transfers ever need to hold stock across a real draft window (not
just draft → immediate dispatch), that's the trigger to give transfers their own
reservation rows; until then, `StockAvailabilityValidator` stays a thin net against sales'
existing reservations only.

`guardSufficientStock` acquires its own `PRODUCT` advisory lock and fetches its own
balances internally, exactly like the "self-protecting updaters" below — it does not take
pre-fetched balances as a parameter. This means `getLatestBalances` runs once here and
again inside `StockTransferStockUpdater.consumeStockForDispatch` moments later: a real,
accepted duplicate query, not an oversight. The alternative (caller fetches once, passes
the snapshot into both the guard and the updater) was tried and reverted — it would make
guard correctness depend on every future caller remembering to lock-then-fetch in the
right order before calling it, which is the exact class of bug the mutation lock below was
already introduced to eliminate (see the git history on `SaleStockUpdater`/
`StockTransferStockUpdater`: transfer dispatch/cancel used to have no `PRODUCT` lock at
all, only sale confirm did, until locking was moved inside the updaters themselves so
protection can't depend on which caller remembers to ask for it).

## Destination location product resolution

Dispatch lines store the **source** location's `locationProductId`. When writing to the destination schema (receipt lines, stock entries, stock movements), the destination `locationProductId` must be looked up by `productId` via `LocationProductDataFetcher.findIdentityByProductId`. Never copy the source `locationProductId` to the destination.

## Kafka processor conventions

Only `StockTransferDispatchedHandlerForKafka` and `StockTransferCancelledHandlerForKafka` (the producer-side beans) implement `EventReissueHandler` — matching the codebase-wide rule that each `eventType` has exactly **one** reissue handler (enforced at startup by `TransactionEventCoverageCheck`). Their `sourceDocumentId` convention is the **transfer order ID** (org schema). `StockTransferDispatchedInventoryProcessor` and `StockTransferCancelledInventoryProcessor` are consumer-only `InventoryEventProcessor`s and must **not** implement `EventReissueHandler`: `EventRetryService` already covers consumer-side recovery by re-sending the original record from the DLT back onto the topic when a consumer group is known, falling back to a reissue handler only when the event was never published at all. A second reissue handler per event type is unreachable (`EventRetryService` looks up exactly one by event type name) and fails the startup coverage check.

## Locking

**Stock mutation lock (self-protecting updaters):** `SaleStockUpdater` and `StockTransferStockUpdater` acquire `LockNamespaces.PRODUCT` on the affected `locationProductId`s internally, before reading any `stock_entry` rows for modification. Callers do not need to acquire this lock — it is impossible to call these updaters without getting the protection. This covers sale confirm, sale void, transfer dispatch, and transfer cancel.

**Order-level dispatch lock:** `StockTransferDispatchService.dispatch` acquires `LockNamespaces.STOCK_TRANSFER_ORDER` on the dispatch entity ID before loading draft lines. This serializes concurrent dispatch calls on the same order — both can pass the `DRAFT` status check before either holds the lock; the second will re-check after the first commits and see `DISPATCHED`, then throw.

**Receipt confirmation lock:** `StockTransferReceiptService.confirmLine` and `unconfirmLine` both acquire `LockNamespaces.STOCK_TRANSFER_ORDER` before touching receipt lines.
