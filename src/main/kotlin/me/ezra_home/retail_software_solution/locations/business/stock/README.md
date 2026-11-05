# Stock

Stock is a set of FIFO layers (`StockEntryEntity`) and an append-only history of movements on them
(`StockMovementEntity`). Nothing in the history is edited or deleted; an undo is a new movement.

## Layers

- One layer per inbound: a purchase delivery line, opening stock, a stock adjustment increase or a transfer in.
  `StockInboundLayers.newLayer` creates it with `priority = 0`, its `batchCost` and `costRemaining`.
- A layer has a `status`: `ACTIVE`, or `REPLACED` once a revision has restored and drained it. A replaced layer is dead:
  it holds nothing, is never drawn from, and the movements on it are settled history, so flows that undo a draw skip
  draws that sit on a replaced layer. Nothing sets it back.
- Draw order is `priority` ascending, then `createdOn`, nulls last (`stockEntryFifoComparator`). Only layers with
  `storageQuantityRemaining > 0` are drawn from (`StockEntryRepository.findFifoEntriesForProducts`).
- The balance of a product is the sum of `storageQuantityRemaining` over its layers (`StockBalanceFetcher`), not a sum
  over movements.
- A take is priced against the layer's exact batch proportion, not against what is left, so rounding does not compound
  across takes (`FifoTakePlanner`).

## Movements

- A draw writes one movement per layer it touches, with the layer id, the quantity and the cost moved.
- A reversal (`FifoStockConsumer.restore`) puts the quantity and cost back on the layer and writes a movement of the
  reversal type that copies the document and detail references of the movement it undoes.
- At most one movement exists per document reference, detail reference, type, product, layer and reason: the unique
  index `uq_stock_movement_doc_detail_type_product_entry_reason`. Retried processors rely on it.
- A movement may carry a free-text `context` (`stock_movement.context`, shown in the stock history) for what the document
  and detail references don't say. Purchase deliveries write `Purchase <reference>` on `PURCHASE_RECEIVED` movements
  (`StockMovementContexts`), and the restore and drain of a revision write the stock revision's reference. Movements
  written before deliveries carried it get theirs from the purchase movement context backfill.
- Updaters take the `LockNamespaces.PRODUCT` advisory lock for the products they change before touching layers.
- Reservations are per product, not per layer. Availability is the balance minus the reserved total
  (`StockAvailabilityValidator`).

## What a movement type is

Every `MovementType` declares its `stockDirection`, so a new type cannot be added without saying whether it puts units
in or takes them out (`increasesStock()`, `decreasesStock()`).

| Role | Types |
|---|---|
| Decreases stock: a draw from a layer | `SALE`, `STOCK_ADJUSTMENT_DECREASE`, `TRANSFER_OUT`, `SUPPLIER_RETURN` |
| Decreases stock: zeroes a layer a revision replaces (`StockEntryDrainer`), not a consumer | `STOCK_REVISION_ZEROING` |
| Increases stock, a reversal: undoes a draw, listed in `MovementType.reversals` | `SALE_VOID` (of `SALE`), `TRANSFER_CANCELLED` (of `TRANSFER_OUT`) |
| Increases stock, a reversal of a draw of any type, written by a revision's restore (`StockEntryRestorer`) | `REVERSED_BY_REVISION` |
| Increases stock, a layer creator: how a layer was born | `PURCHASE_RECEIVED`, `OPENING_STOCK`, `TRANSFER_IN`, `STOCK_ADJUSTMENT_INCREASE` |
| Increases stock, neither: `CUSTOMER_RETURN` | |
| Cannot be reversed: `MovementType.movementsThatCannotBeReversed` | `CUSTOMER_RETURN`, `SUPPLIER_RETURN` |

## Net consumption of a layer

`StockEntryConsumptionFinder.netConsumptions(stockEntryIds)` answers what still holds units on the given layers.

- It reads every movement on the layers itself. A reversal left out of a partial list would make its draw look live.
- A reversal belongs to the draw with the same layer, document reference and detail reference
  (`EntryConsumptionSourceDocument`). Each document family has its own reference sequence, so these three identify a
  draw whatever its type.
- The net of a draw is its quantity and cost minus its reversals. Draws that net to zero, such as a voided sale or a
  cancelled transfer, are left out. The rest come back oldest first.
- A reversal with no draw, or a draw reversed by more than it took, is a corrupt history. All such problems are reported
  together in one `RtsGenericException`, whose payload lists `Stock entry <reference>: ...` lines.
- Movements that increase stock without being a reversal are ignored. `SUPPLIER_RETURN` decreases stock but has no
  reversal and cannot be replayed through FIFO, so a caller rejects layers holding the cannot-be-reversed types first.
- `REVERSED_BY_REVISION` pairs with its draw like any reversal. `STOCK_REVISION_ZEROING` decreases stock and has no
  reversal, so on a replaced layer it comes back as a consumption; `StockEntryDrainer` rejects replaced layers before
  asking.

## Restoring and draining layers

`StockEntryResetter.reset(stockEntryIds, stockRevisionReferenceNumber)` empties the layers a revision replaces. It takes
the `LockNamespaces.PRODUCT` advisory lock for the layers' products itself, before it reads any layer or movement, so
it does not rely on the caller having locked; an orchestrator locking again in the same transaction is harmless. It
loads and saves, and hands the two steps to `StockEntryRestorer` (reverses the net consumptions) and
`StockEntryDrainer` (zeroes each layer and rejects layers already zeroed). It creates no replacement, so a caller must
write one in the same transaction or the stock is lost.

- Every net consumption from `StockEntryConsumptionFinder` is reversed for its net quantity and cost by one
  `REVERSED_BY_REVISION` movement, whatever type the draw had. The movement copies the draw's document and detail
  references, so a revision never writes a `SALE_VOID` or `TRANSFER_CANCELLED` that the void and cancel flows would read
  as their own. Cost and units go back onto the layer.
- Each layer is then drained to zero by one `STOCK_REVISION_ZEROING` movement carrying the layer's whole remaining cost,
  so the drain is not a write-off. It keeps the layer's own document and detail references, so it reads under the
  delivery line the layer came from. A zero-quantity layer (a replacement of zero) has nothing to drain and gets no
  movement.
- The restore movements carry the context "Restored for stock revision <reference>" and the drain movement "Drained by
  stock revision <reference>" (`StockMovementContexts`). The reference is the `stock_revision` reference, drawn by the
  caller before the reset because the row itself is saved last.
- Every layer reset is marked `REPLACED`, a zero-quantity one included. A layer that is already `REPLACED` is rejected, so
  a second run cannot restore or drain it again.
- A consumer whose type is in `MovementType.movementsThatCannotBeReversed` (`SUPPLIER_RETURN`, `CUSTOMER_RETURN`) is
  rejected before anything is written, all together in one `RtsGenericException` with the lines as `payload`.
- The product balance on each movement is the running balance, so the drain leaves it short until the replacement
  layer exists.
- Returns the consumptions that were restored, oldest first, for the replay.

## Questions about a delivery line

`DeliveryLineStockInspector` (in `api`) finds a delivery line's layers by its document reference (the delivery) and
detail reference (the line):

- `hasTransferredOut`: the count of `TRANSFER_OUT` movements minus `TRANSFER_CANCELLED` movements on those layers is
  above zero. A dispatched and cancelled transfer does not count.
- `hasMovementsThatCannotBeReversed`: any movement of a type in `movementsThatCannotBeReversed` exists on those layers.
