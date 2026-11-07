# Stock revision

What happens to stock when a source document is revised after other documents have already used its stock.
The first half is the story that connects the steps; the second half holds the rules of each step. What a movement
type is, the layer and movement basics, and the unique index are in `../README.md`.

## The story

Stock exists because a source document says so. A delivery line says "10 units arrived at 8.00 each", and the stock
gets a **layer** (a batch, a `StockEntryEntity`) of 10 units worth 80.00. Sales, adjustments and transfers then draw
from layers, oldest first (FIFO), and every draw is recorded as a movement that names the layer it came from and the
cost it took.

Now someone finds the delivery line was wrong: it was really 6 units at 10.00 each. The line is revised
(the old line becomes `REVISED`, a corrected sibling line is created), and the layer it created can no longer be
trusted (the layer becomes `REPLACED`). But two sales have already drawn from it, and their recorded cost
(which feeds profit and tax) rests on a layer that was wrong. Those sales did happen, so they need to be
redone against the corrected stock.

Movements are never edited or repointed, because the stock history is append-only. The revision adds new movements
that undo the old ones and then redo the draws, so the history shows exactly what happened and when.

## The walk-through

Layer **STE2** came from delivery line DLV1/DLL1: 10 units at 8.00 = 80.00. Sale **SAL1** took 4 (32.00), sale
**SAL2** took 3 (24.00). A later layer, **STE3**, holds 50 units at 9.00. The line is revised to 6 units at 10.00.

| Step | What is written | STE2 | Notes |
|---|---|---|---|
| Before | SAL1 draws 4, SAL2 draws 3 | 3 left | |
| 1. Put back | `REVERSED_BY_REVISION` for 4 (32.00) and for 3 (24.00) | 10 left, 80.00 | Only so the layer can be emptied. |
| 2. Drain | `STOCK_REVISION_ZEROING` for 10 (80.00), layer marked `REPLACED` | 0 left | Not a write-off: it takes the layer's whole cost out. |
| 3. Replace | A new layer for 6 units at 10.00 = 60.00 | | Sorts ahead of ordinary layers, so it is drawn first. |
| 4. Draw again | SAL1 draws 4 from the new layer (40.00). SAL2 draws 2 from the new layer (20.00) and 1 from STE3 (9.00) | | New layer is now empty. |

The result: SAL1's cost went from 32.00 to 40.00 and SAL2's from 24.00 to 29.00. Those two numbers per consumer
(`oldCost`, `newCost`) are what the rest of the system needs to correct things that were calculated from the old cost.

Things that follow from this:

- **A short replacement is normal.** The new layer had 6 units for 7 needed. The consumers fill the rest from later
  layers in FIFO order. The revision fails only when every layer together cannot cover a consumer.
- **A consumer is a document line (or a whole adjustment), not a layer.** A revision replaces one layer, so each consumer
  is put back and drawn again once, as the one quantity it held on that layer.
- **Only the quantity that sat on the replaced layers is redone.** A sale that also drew from other layers keeps
  that part untouched.
- **A document may draw again from a layer it already drew from.** The new draw carries the stock revision's reference, so
  it stays distinct from the earlier draw, and net consumption adds all of a document's draws and reversals on a layer.
- **Some consumers cannot be redone.** A supplier return is tied to one delivery's supplier and cost, so a revision of
  stock it drew from is refused up front.

## Who does what

All of it runs in one transaction under the product lock, in this order:

| Step | Class | Rules below |
|---|---|---|
| Work out who still holds stock on the replaced layer | `NetConsumptionCalculator` (returns `StockEntryConsumption`s) | Net consumption of a layer |
| 1 and 2: put back, then drain | `StockEntryResetter`, using `StockEntryRestorer` and `StockEntryDrainer` | Restoring and draining layers |
| 3: create the corrected layer | `StockReplacementLayerRecorder` (in `stock/api`) | The replacement layer |
| 4: draw again | `StockEntryReconsumer` (loads and saves), `ConsumerReplayer` (the pure part, fed by a `ReplayRequest`) | Replaying consumers |

Step 3 must happen before step 4, because the draw again looks for the replacement layer among the layers with
stock left. Nothing here creates the replacement for you: after step 2 the stock is short until step 3 runs.
The resetter and the reconsumer join the caller's transaction and do not enforce one, so the caller wraps all four steps in a
single transaction.

## Net consumption of a layer (`NetConsumptionCalculator`)

`NetConsumptionCalculator.calculateNetConsumptions(stockEntryId)` answers what still holds units on the one layer a revision replaces.

- It reads every movement on the layer itself, in no particular order. A reversal left out of a partial list would make its draw look live.
- Draws and reversals are matched by origin: the document reference and detail reference (`StockMovementOrigin`).
  Each document family has its own reference sequence, so these two identify a consumer on the layer whatever the type
  of its draws.
- A consumer may hold more than one draw on a layer, because a replay can land on a layer the consumer already drew from.
  The calculator first collects all draws of an origin, then all its reversals, then subtracts the reversed total
  from the drawn total, so each reversal counts once. The result is one `StockEntryConsumption` per origin, carrying
  its `StockConsumer` (document and detail references, movement type and adjustment details), the `QuantityAsEntered` of its
  earliest draw (unit, factor and the quantity text as typed; it only labels the restore and replay movements), the layer's
  product, and the order of its first draw on any layer of the product
  (`OrderOfConsumption`; a replayed draw is stamped with the replay time, so the layer's own earliest draw would drift). The detail reference is empty for a stock adjustment, which has no lines.
- Consumers that net to zero, such as a voided sale or a cancelled transfer, are left out. The rest come back
  oldest first.
- A reversal with no draw, a draw reversed by more than it took, or draws of one origin on a layer that differ in
  movement type or adjustment details, is a corrupt history. A layer serves many documents in different units; only
  one document disagreeing with itself is rejected. All such problems are reported together in one `RtsGenericException`,
  whose payload lists `Stock entry <reference>: ...` lines.
- Movements that increase stock without being a reversal are ignored. `SUPPLIER_RETURN` decreases stock but has no
  reversal and cannot be replayed through FIFO, so a caller rejects a layer holding the cannot-be-reversed types first.
- `REVERSED_BY_REVISION` pairs with its draw like any reversal. `STOCK_REVISION_ZEROING` decreases stock but drains a
  layer rather than serving a consumer (`MovementType.isConsumption()`), so it is never a consumption, whether or not
  the layer is already replaced.

## Restoring and draining layers (`StockEntryResetter`)

`StockEntryResetter.reset(stockEntryId, stockRevisionReferenceNumber)` empties the one layer a revision replaces. It takes
the `LockNamespaces.PRODUCT` advisory lock for the layer's product itself, before it reads the layer or its movements, so
it does not rely on the caller having locked; an orchestrator locking again in the same transaction is harmless. It
loads and saves, and hands the two steps to `StockEntryRestorer` (reverses the net consumptions) and
`StockEntryDrainer` (zeroes the layer and writes the drain movement). It rejects a layer that is already replaced before it restores anything,
and marks every layer it resets replaced, even one with nothing to drain. It creates no replacement, so a caller must
write one in the same transaction or the stock is lost.

- Every net consumption from `NetConsumptionCalculator` is reversed for its net quantity and cost by one
  `REVERSED_BY_REVISION` movement, whatever type the draw had. The movement copies the draw's document and detail
  references, so a revision never writes a `SALE_VOID` or `TRANSFER_CANCELLED` that the void and cancel flows would read
  as their own. Cost and units go back onto the layer.
- Each layer is then drained to zero by one `STOCK_REVISION_ZEROING` movement carrying the layer's whole remaining cost,
  so the drain is not a write-off. Its document reference is the stock revision's reference, its detail reference is
  the layer's own (the replaced delivery line's), and the revision's reference is also stored in
  `stock_movement.stock_revision_reference`. The movement is recorded in the product's storage unit, because a layer keeps no
  unit of its own and the text it shows is rendered in the current units. A zero-quantity layer (a replacement of zero) has nothing to drain and gets no
  movement.
- The restore movements carry the context "Restored for stock revision <reference>" and the drain movement "Drained by
  stock revision <reference>, which revised delivery <delivery>/<line>" (`StockMovementContexts`; "delivery" only for a
  purchase layer). The reference is the `stock_revision` reference, drawn by the
  caller before the reset because the row itself is saved last.
- Every layer reset is marked `REPLACED`, a zero-quantity one included. A layer that is already `REPLACED` is rejected, so
  a second run cannot restore or drain it again.
- A consumer whose type is in `MovementType.movementsThatCannotBeReversed` is rejected before anything is written, all
  together in one `RtsGenericException` with the lines as `payload`. Only `SUPPLIER_RETURN` can reach this check, because a
  consumer is always a decrease. `CUSTOMER_RETURN` increases stock, so it is never a consumer; a layer holding one is
  refused before a revision starts, by `DeliveryLineStockInspector.hasMovementsThatCannotBeReversed`.
- The product balance on each movement is the running balance, so the drain leaves it short until the replacement
  layer exists.
- Every restore and drain movement stores the revision's reference in `stock_movement.stock_revision_reference`, so one
  query finds everything a revision wrote.
- Returns the consumptions that were restored, oldest first, for the replay.

## The replacement layer (`StockReplacementLayerRecorder`)

Recorded through `StockReplacementLayerRecorder` (in `stock/api`, so the revision flow in another domain can call it): it
keeps the original delivery's document reference, takes the new line's reference as detail reference, and the caller
supplies source type, movement type and `priority`. A low priority sorts it ahead of ordinary layers, which is why
consumers draw from it first. Its movement context names the stock revision (`StockMovementContexts`).

## Replaying consumers (`StockEntryReconsumer`)

A replaced layer is emptied in two steps (`StockEntryResetter`): its consumers' draws are put back on it, only so the
layer can then be drained to zero. The consumers still need that stock, so `StockEntryReconsumer.reconsume(
consumptionsOnReplacedLayer, replacementStockEntryId, stockRevisionReferenceNumber)` draws it again, after the replacement layer is saved. It
refuses a replacement that is missing, already replaced or of another product. It checks by id because a zero-quantity
replacement is legitimate but is not among the layers with stock left, and without the check other layers could cover the
replay and the replacement saved later would overstate the stock. It
takes the product advisory lock itself and works in two phases. First `ConsumerReplayer` plans every consumer on
`StockEntryFifoDto` copies of the layers, in FIFO order, and writes nothing. It returns one `ConsumerReplayPlan` per
consumer (the draw to make and the layers to take from) and collects every problem. If there is any problem,
`reconsume` throws before a layer is changed. Otherwise it applies the plans in order with
`FifoStockConsumer.applyTakes`, which draws the layers down and creates the movements, and saves the touched layers and
the new movements. The replacement sorts ahead of ordinary layers, so consumers take from it first. A replacement
smaller than what was drawn is normal: the rest of each consumer's quantity comes from the next layers in order. Only
when all layers together cannot cover a consumer does the replay fail. Because the replayer changes nothing it is given,
a dry run (the revision preview) can call it directly.

- The replayer takes the consumptions of one replaced layer and rejects consumptions of several, since a revision replaces one
  layer at a time. Each consumption replays as one draw of its net quantity, oldest draw first, by the creation time (then id)
  of its first draw on any layer, whatever order the caller lists them in. A consumer that also drew from other layers keeps that part
  as it was, and a later revision of one of those layers replays its portion there.
- A replayed movement keeps the draw's type, document and detail references, whole-line quantity text and adjustment
  category, reason and note, carries the context "Replayed after stock revision <reference>", and stores the revision's
  reference in `stock_movement.stock_revision_reference`, so it does not collide with the document's earlier draw on the same layer.
- Each plan reports `oldCost` (the net cost put back on the replaced layer) and `newCost` (the sum of its takes). They cover only what was drawn from the replaced
  layer: a consumer that also drew from other layers keeps that part as it was.
- All problems are collected before anything is written, and `reconsume` throws one `RtsGenericException` whose `payload`
  lists them: a consumer the layers cannot cover (it draws nothing, so the rest still replay and report).
