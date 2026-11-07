# Stock

Stock is a set of FIFO layers (`StockEntryEntity`) and an append-only history of movements on them
(`StockMovementEntity`). Nothing in the history is edited or deleted; an undo is a new movement.

## Layers

- One layer per inbound: a purchase delivery line, opening stock, a stock adjustment increase or a transfer in.
  `StockInboundLayers.newLayer` creates it with the spec's `priority` (0 unless the caller sets one), its `batchCost` and
  `costRemaining`. Its `activatedOn` is the time it is saved unless the spec carries one: a revision's replacement layer takes the
  revised layer's, which is how it keeps that layer's place in the draw order. `StockIncreaseRecorder.record` returns the new layer ids
  in spec order.
- A revision's replacement layer is recorded through `StockReplacementLayerRecorder` (see `../stock_revision/README.md`).
- A layer has a `status`: `ACTIVE`, or `REPLACED` once a revision has restored and drained it. A replaced layer is dead:
  it holds nothing and is never drawn from. Nothing sets it back.
- Draw order is `priority` ascending, then `activatedOn`, then `createdOn`, nulls last (`stockEntryFifoComparator`). Only layers with
  `storageQuantityRemaining > 0` are drawn from (`StockEntryRepository.findFifoEntriesForProducts`).
- The balance of a product is the sum of `storageQuantityRemaining` over its layers (`StockBalanceFetcher`), not a sum
  over movements.
- A take is priced against the layer's exact batch proportion, not against what is left, so rounding does not compound
  across takes (`FifoTakePlanner`).

## Movements

- A draw writes one movement per layer it touches, with the layer id, the quantity and the cost moved.
- A reversal (`FifoStockConsumer.restore`) puts the quantity and cost back on the layer and writes a movement of the
  reversal type that copies the document and detail references of the movement it undoes.
- At most one movement exists per document reference, detail reference, type, product, layer, reason and stock revision
  reference (null except on a revision's drain and replayed draws): the unique index
  `uq_stkmv_doc_detail_type_product_entry_reason_revision`. Retried processors rely on it.
- A movement may carry a free-text `context` (`stock_movement.context`, shown in the stock history) for what the document
  and detail references don't say. Purchase deliveries write `Purchase <reference>` on `PURCHASE_RECEIVED` movements
  (`StockMovementContexts`), and a stock revision's restore, drain and replay write the revision's reference (see `../stock_revision/README.md`). Movements
  written before deliveries carried it get theirs from the purchase movement context backfill.
- Updaters take the `LockNamespaces.PRODUCT` advisory lock for the products they change before touching layers.
- Reservations are per product, not per layer, and live in the `stock_reservation` domain. Availability is the balance minus the
  reserved total (`StockAvailabilityValidator`, `StockAvailabilityFetcher`); those two stay here because they read the balance.

## Layout of `api/`

Other domains reach stock only through `stock/api`, grouped by what the caller does:

| Subpackage | What it holds |
|---|---|
| `updater/` | The updaters that change stock for a document (sale, transfer dispatch and receipt, adjustment, purchase delivery, opening stock) and their request and result types |
| `revision/` | What a stock revision writes through: `StockRevisionCommitter`, `StockReplacementLayerRecorder`, and the drafts, plans and DTOs they take and return |
| `read/` | Read access: balances, entry and movement DTOs and fetchers, the movement history, the delivery line inspector and the purchase movement context backfill store |
| `fifo/` | FIFO planning types: `FifoTakePlanner`, `FifoConsumptionRequest`, `PlannedTake`, `FifoLayer`, `StockEntryFifoDto` |
| `availability/` | The availability guard and fetcher |
| (root) | Vocabulary shared by all of the above: adjustment category and details, entry status, movement contexts |

`ArchitectureTest` treats every package whose name contains `.api` as public, so the subpackages need no rule of their own.

## What a movement type is

Every `MovementType` declares its `stockDirection`, so a new type cannot be added without saying whether it puts units
in or takes them out (`stockDirection`, `decreasesStock()`).

| Role | Types |
|---|---|
| Decreases stock: a draw from a layer | `SALE`, `STOCK_ADJUSTMENT_DECREASE`, `TRANSFER_OUT`, `SUPPLIER_RETURN` |
| Decreases stock: zeroes a layer a revision replaces (`StockEntryDrainer`), not a consumption | `STOCK_REVISION_ZEROING` |
| Increases stock, a reversal: undoes a draw, listed in `MovementType.reversals` | `SALE_VOID` (of `SALE`), `TRANSFER_CANCELLED` (of `TRANSFER_OUT`) |
| Increases stock, a reversal of a draw of any type, written by a revision's restore (`StockEntryRestorer`) | `REVERSED_BY_REVISION` |
| Increases stock, a layer creator: how a layer was born | `PURCHASE_RECEIVED`, `OPENING_STOCK`, `TRANSFER_IN`, `STOCK_ADJUSTMENT_INCREASE` |
| Increases stock, neither: `CUSTOMER_RETURN` | |
| Cannot be reversed: `MovementType.movementsThatCannotBeReversed` | `CUSTOMER_RETURN`, `SUPPLIER_RETURN` |

## Stock revisions

When a source document is revised after others drew from its layers, the layers are put back, drained, replaced and
drawn again. The story, the worked example and the rules of each step are in `../stock_revision/README.md`.

## Questions about a delivery line

`DeliveryLineStockInspector` (in `api`) finds a delivery line's layers by its document reference (the delivery) and
detail reference (the line):

- `hasTransferredOut`: the count of `TRANSFER_OUT` movements minus `TRANSFER_CANCELLED` movements on those layers is
  above zero. A dispatched and cancelled transfer does not count.
- `hasMovementsThatCannotBeReversed`: any movement of a type in `movementsThatCannotBeReversed` exists on those layers.
