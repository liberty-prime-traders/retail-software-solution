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

## Response assembly after each write

`StockTransferResponseAssembler.build` is read-only and always re-reads both schemas via `REQUIRES_NEW`. It must never be called right after each write to either schema in the *same* still-open transaction — that write won't be visible yet to a `REQUIRES_NEW` connection. Every service that mutates its own ambient schema and then needs a response uses a dedicated assembler method instead, passing in the entity/lines it just touched directly: `buildDispatchOnly` (draft line add/update/remove, dispatch creation, and the draft-to-dispatch transition — all source-schema) and `buildDispatchAndReceipt` (receipt line confirm/unconfirm/complete — destination-schema; the dispatch side is still read from the foreign source schema via the gateway, which is safe since nothing was written there in the same transaction).

`ReconciledTransferLineFetcher` (not `StockTransferSchemaGateway`) is what those services call for their own-schema draft/dispatch line reads. It exists specifically so `StockTransferSchemaGateway` can stay uniform: every gateway method is `REQUIRES_NEW` cross-schema access, no exceptions. `ReconciledTransferLineFetcher`'s methods are the opposite — `Propagation.MANDATORY`, meaning they refuse to run without an already-active location-schema transaction and always join it. `StockTransferSchemaGateway.buildDispatchWithLines` itself calls into `ReconciledTransferLineFetcher` (MANDATORY is happy to join the gateway's own REQUIRES_NEW transaction) to avoid duplicating the entity-to-`ReconciledTransferLine` mapping.

## Gateway pattern

`StockTransferSchemaGateway` is a pure data access component for the source location schema. It must not publish Kafka events — event publishing belongs in the service layer, where transaction scope is controlled. This is the rule that prevents the Kafka event from firing before the org-schema write completes.

## Event sequencing rule

`StockTransferDispatchedEvent`, `StockTransferCancelledEvent` and `StockTransferReceiptCompletedEvent` must be published **after** the corresponding org-schema status update, within the outer service transaction. They fire on `AFTER_COMMIT` of that transaction. If they were published inside a `REQUIRES_NEW` gateway call, they would fire before the org write — leaving the two schemas inconsistent.

## In-transit stock

Dispatch drains the source's FIFO layers (`TRANSFER_OUT`) and writes nothing at the destination. Stock a `DISPATCHED` transfer carries is its dispatch lines (`storageQuantityDispatched`, `costDispatched`): on hand at neither location, and no stock table holds it.

`StockTransferReceiptService.completeTransfer` credits the destination in its own transaction: it reads the dispatch lines from the source schema and calls `StockTransferReceiptStockUpdater.recordTransferReceipt`, which writes one `TRANSFER_IN` layer per line. `confirmLine` and `unconfirmLine` write no stock, so unconfirming never needs a reversal. `recordTransferReceipt` takes the `PRODUCT` lock on the destination products first, then skips lines that already have a `TRANSFER_IN` movement under the dispatch's reference, so a repeated call credits nothing twice even without the caller's order lock.

Cancelling a dispatched transfer restores the source only (`TRANSFER_CANCELLED`), because the destination holds nothing to reverse. The restore runs inside the cancel request, in the source-schema transaction that marks the dispatch `CANCELLED` (`StockTransferSchemaGateway.cancelDispatch`), before the order status is written and before the event is published. A history that cannot be restored (a layer reversed more than it was drawn) throws `BlockedChangeException` there, so the cancel fails and the order stays `DISPATCHED`. The order status is written afterwards in the org schema, so a failure between the two leaves a `CANCELLED` dispatch under a `DISPATCHED` order. The ledger follows the same path: dispatch moves the cost into `INVENTORY_IN_TRANSIT`, completion moves it into the destination's `INVENTORY`, and cancel returns it to the source's (see "Stock transfer postings" in the ledger README).

## FIFO allocation

`StockTransferFifoAllocator` splits a draft line into one or more dispatch lines, one per cost group. A single draft line for a product with stock at two different rates produces two dispatch lines. This means any consumer of those lines must accumulate a running balance per `locationProductId` across lines — not use a single pre-fetched balance for each line — because multiple lines for the same product share the same balance accumulator. Both `StockIncreaseRecorder.record` (called by `StockTransferReceiptStockUpdater.recordTransferReceipt`, and shared with the purchase-delivery and opening-stock updaters) and `consumeStockForDispatch` in `StockTransferStockUpdater` follow this: they thread the running balance per `locationProductId` across a product's own lines.

A transfer draft line stores the typed `userProvidedQuantity`, its `unitId` and the `storageUnitsPerUserProvidedUnit` captured when the line was added or its unit changed (there is no `base_unit_id`: the product's base unit is immutable). The allocator plans the whole line once with `FifoTakePlanner.planTakes` (the same walk and cost rule `FifoStockConsumer` uses) and groups the planned takes into **cost groups: runs of consecutive FIFO layers whose derived rate matches at scale 4**. A run, not a group-by, so consuming the groups one after another walks the layers in the order they were planned. Each group becomes one `StockTransferDispatchLineEntity`:

- `storageQuantityDispatched` is the group's exact share and `costDispatched` the cost consumed for it (the sum of its takes' `moved_cost`), so the groups' costs sum to the cost consumed and a layer's cost is conserved across locations. `unitCost` is the derived rate (`costDispatched ÷ storageQuantityDispatched`, scale 10) for display; a total is never recovered by multiplying it back.
- `wholeLineUserProvidedQuantity`, `unitId` and `storageUnitsPerUserProvidedUnit` are the **draft line's whole-line values, identical on every cost-group row of that line** and never a share of it (the column is `whole_line_user_provided_quantity`, and the Kafka line DTO carries the same name). Never sum `wholeLineUserProvidedQuantity` across a line's rows. Its wire field `quantity` is that whole figure, and `storageQuantity` / `storageBreakdownText` carry the row's own share.
- The source consumes, and the destination receives, exactly `storageQuantityDispatched` (`StockTransferReceiptLineStockRequest`), so neither side converts anything.

The movements a dispatch line produces use the dispatch's reference as `document_reference` and the dispatch line's reference as `detail_reference`, which also makes the several cost-group lines of one transfer distinct in the unique indexes. Cancel and receipt idempotency check `detail_reference` within the dispatch's `document_reference`.

## Storage unit match

Dispatch quantities are in the source product's storage unit (the root of its unit group), so the destination product must count stock in the same root. Two locations can hold different base units for the same org product, so the check compares roots, not product ids. `StockTransferDispatchService.dispatch` checks it before any stock event (`guardDestinationCountsInSameStorageUnit`, reading the destination's base units through `StockTransferSchemaGateway.readBaseUnitIdsByOrgProductId`); `StockTransferReceiptStockUpdater` re-checks it as a backstop before creating any layer.

## Reservation-aware dispatch guard

`StockTransferDispatchService.dispatch` acquires the `PRODUCT` advisory lock on the draft
lines' `locationProductId`s before reading any `stock_entry` rows — including the
`getFifoEntriesByProduct` read that feeds `StockTransferFifoAllocator`'s cost-group split.
This closes the window where a concurrent sale or transfer could otherwise change the FIFO
layers between that read and the dispatch lines being persisted, which would leave the
recorded `costDispatched` / `totalDispatchedCost` inconsistent with what `StockTransferStockUpdater`
actually decrements moments later. Before `StockTransferFifoAllocator` runs, `dispatch` also
calls `StockAvailabilityValidator.guardSufficientStock` (`stock/api/`) with each draft line's
exact storage quantity. This nets on-hand balance against the **total** of any live sale
reservations (`StockReserver.loadReservationBreakdown(...).total`) for the same
`locationProductId`, compares exactly with no tolerance, and throws before allocation if a transfer would take
stock a sale has already reserved. Unlike `SaleValidator.guardSufficientStockForSale`, this call never
excludes anything — draft transfers hold no reservation of their own today, so there is no
"self" to exclude. If transfers ever need to hold stock across a real draft window (not
just draft → immediate dispatch), that's the trigger to give transfers their own
reservation rows; until then, `StockAvailabilityValidator` stays a thin net against sales'
existing reservations only.

`guardSufficientStock` acquires its own `PRODUCT` advisory lock and fetches its own
balances internally, exactly like the "self-protecting updaters" below — it does not take
pre-fetched balances as a parameter. Since `dispatch` already holds the same `PRODUCT` lock
by the time `guardSufficientStock` runs (see above), this re-acquire is a harmless no-op —
`pg_advisory_xact_lock` stacks within a transaction. This means `getLatestBalances` runs once
here and again inside `StockTransferStockUpdater.consumeStockForDispatch` moments later: a
real, accepted duplicate query, not an oversight. The alternative (caller fetches once, passes
the snapshot into both the guard and the updater) was tried and reverted — it would make
guard correctness depend on every future caller remembering to lock-then-fetch in the
right order before calling it, which is the exact class of bug the mutation lock below was
already introduced to eliminate (see the git history on `SaleStockUpdater`/
`StockTransferStockUpdater`: transfer dispatch/cancel used to have no `PRODUCT` lock at
all, only sale confirm did, until locking was moved inside the updaters themselves so
protection can't depend on which caller remembers to ask for it).

## Draft line availability display

`ReconciledTransferLineFetcher.draftLines` populates each `ReconciledTransferLine`'s
`quantityAvailable` (in the product's base unit, rounded down) via `StockAvailabilityFetcher.fetch` (`stock/api/`) — the same shared
component `LocationProductForSaleAssembler` and `SaleSessionStockOverlay` use for their own
availability displays. The underlying `quantityAvailable` nets on-hand balance against the
**total** of live sale reservations, with no exclusion, matching `guardSufficientStock`'s
behavior above: draft transfer lines hold no reservation of their own to exclude. Unlike
`guardSufficientStock`, this call takes **no** `PRODUCT` lock — it's a display-only read for
the pre-dispatch review screen, not a guard gating a mutation, so serializing concurrent
viewers against it would be pointless and could contend with the real dispatch lock for no
reason. `dispatchLines` and the post-dispatch `toReconciledLines` (in
`StockTransferDispatchService`) leave the field `null`: once stock has actually moved, an
available-quantity snapshot for the now-consumed draft quantity is no longer meaningful.

## Destination location product resolution

Dispatch lines store the **source** location's `locationProductId`. When writing to the destination schema (receipt lines, stock entries, stock movements), the destination `locationProductId` must be looked up by `orgProductId` via `LocationProductDataFetcher.findIdentityByOrgProductId`. Never copy the source `locationProductId` to the destination.

## Kafka processor conventions

`StockTransferDispatchedHandlerForKafka`, `StockTransferCancelledHandlerForKafka` and `StockTransferReceiptCompletedHandlerForKafka` (the producer-side beans) are the only `EventReissueHandler`s — matching the codebase-wide rule that each `eventType` has exactly **one** reissue handler (enforced at startup by `TransactionEventCoverageCheck`). The dispatched and cancelled handlers' `sourceDocumentId` is the **transfer order ID** (org schema); the receipt-completed handler's is the **receipt ID** (destination schema). A reissued dispatched event is dated by the order's `dispatchedAt`; the receipt and cancel dates are not stored, so a reissued receipt-completed or cancelled event is dated the day it is reissued, which can fall in a later fiscal period than the original. This is a known asymmetry, not an oversight to close ad hoc — revisit it if the receipt or cancel date is ever stored.

## Locking

**Stock mutation lock (self-protecting updaters):** `SaleStockUpdater`, `StockTransferStockUpdater` and `StockIncreaseRecorder` acquire `LockNamespaces.PRODUCT` on the affected `locationProductId`s internally, before reading any `stock_entry` rows or the running balance for modification. Callers do not need to acquire this lock — it is impossible to call these updaters without getting the protection. This covers sale confirm, sale void, transfer dispatch, transfer cancel, and every stock increase (purchase delivery, opening stock, stock adjustment, transfer receipt).

**Order lock:** `dispatch`, `cancelTransfer`, `confirmLine`, `unconfirmLine` and `completeTransfer` each call `StockTransferOrderLocker.lockThenFetch`, which takes `LockNamespaces.STOCK_TRANSFER_ORDER` on the **order's** ID and returns a fresh read of the order, so the five operations are mutually exclusive per order and decide on the status read **after** the lock: the second of two concurrent dispatches sees `DISPATCHED`, a completion that waited behind a cancel sees `CANCELLED` and throws, and a cancel that waited behind a completion sees `COMPLETED` and throws. Without that, a cancel racing a completion would restore the source and credit the destination as well. The fresh read relies on the org status write committing before the lock is released: the lock is a transaction-scoped lock on the location connection, while `StockTransferOrderService` runs on the organization transaction manager and commits when each call returns, so no outer organization transaction may wrap these operations.

`completeTransfer` accepts a `COMPLETED` order whose receipt is still `PENDING`: the dispatch and the order commit before the credit does, so a failed credit commit leaves exactly that state, and running the completion again credits it (the credit is idempotent). It refuses an order or dispatch that is `CANCELLED`, and a receipt that is already `COMPLETED`. A second completion that gets past the receipt status check still credits nothing: `recordTransferReceipt` finds the first one's `TRANSFER_IN` movements. The movement unique index does not stop a second credit, because it would create a different stock entry.
