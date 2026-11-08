# Stock revision

What happens to stock when a source document is revised after other documents have already used its stock.
The first half is the story that connects the steps; the second half holds the rules of each step. What a movement
type is, the layer and movement basics, and the unique index are in `../stock/README.md`.

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

The planning is here, in `stock_revision`; the rows are written by `StockRevisionCommitter` in `stock/api/revision`. `DeliveryLineRevisioner` (`stock_revision/api`) runs the whole revision in one
transaction under the product lock, in this order:

| Step | Class | Rules below |
|---|---|---|
| Lock the purchase, then the product; write the corrected delivery line | `PurchaseDeliveryLineReplacer` (`delivery/api`), sent a `PurchaseDeliveryLineReplaceRequest` | The delivery line |
| Work out who still holds stock on the replaced layer | `NetConsumptionCalculator` (returns `StockEntryConsumption`s) | Net consumption of a layer |
| 1 and 2: plan the put back and the drain | `ResetPlanner`, using `StockEntryRestorer` and `StockEntryDrainer`, returns a `StockEntryResetPlan` | Planning the reset |
| 1 and 2: write them | `StockRevisionCommitter.commitReset` | Committing a revision |
| 3: create the corrected layer | `StockReplacementLayerRecorder` (`stock/api/revision`) | The replacement layer |
| 4: plan the draws | `ReplayPlanner`, using `ConsumerReplayer`, returns `ConsumerReplayPlan`s | Planning the replay |
| 4: write them | `StockRevisionCommitter.commitReplay` | Committing a revision |
| Link the layers | `StockRevisionEntity` is saved last, under the reference drawn first | The stock revision row |
| Purchase totals and response | `PurchaseDeliveryRevisionApplier.apply` (delivered quantity, status, payment status); `revise` returns the purchase response | |

Stock data reaches the planners as DTOs from `stock/api` fetchers (`StockMovementFetcher`, `StockEntryFetcher`,
`StockBalanceFetcher`); no planner touches a stock entity or repository. The product's quantity breakdown does not change during a
revision, so `DeliveryLineRevisioner` loads it once (`ProductStockBreakdownProvider`) and passes it to both planners and both commits.
Planning writes nothing, so a dry run (the revision preview) can reuse the planners.

Step 3 must happen before step 4, because the replay looks for the replacement layer among the layers with stock left.
After step 2 the stock is short until step 3 runs. The committer joins the caller's transaction and does not enforce one,
so the orchestrator wraps every step in a single transaction.


## Net consumption of a layer (`NetConsumptionCalculator`)

`NetConsumptionCalculator.calculateNetConsumptions(stockEntryId)` answers what still holds units on the one layer a revision replaces. It reads movements as `StockMovementDto`s through `StockMovementFetcher` (`stock/api`).

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
  one document disagreeing with itself is rejected. All such problems are reported together in one `BlockedChangeException.contactSupport`,
  one `BlockingProblem` per origin, each quoting the stock record reference.
- Movements that increase stock without being a reversal are ignored. `SUPPLIER_RETURN` decreases stock but has no
  reversal and cannot be replayed through FIFO, so a caller rejects a layer holding the cannot-be-reversed types first.
- `REVERSED_BY_REVISION` pairs with its draw like any reversal. `STOCK_REVISION_ZEROING` decreases stock but drains a
  layer rather than serving a consumer (`MovementType.isConsumption()`), so it is never a consumption, whether or not
  the layer is already replaced.

## Planning the reset (`ResetPlanner`)

`ResetPlanner.plan(stockEntryId, consumptions, stockRevisionReferenceNumber, quantityBreakdown)` works out the movements that empty the one layer a
revision replaces and returns them as `StockMovementDraft`s in a `StockEntryResetPlan`. It rejects a layer that is already
replaced before it plans anything (a `BlockedChangeException.contactSupport`; the validator refuses such a line earlier). Because restoring every consumer and then draining always ends a layer empty, the plan holds
only movements; the committer sets the layer to zero and `REPLACED`.

- Every net consumption is reversed for its net quantity and cost by one `REVERSED_BY_REVISION` draft (`StockEntryRestorer`),
  whatever type the draw had. It copies the draw's document and detail references, so a revision never writes a `SALE_VOID`
  or `TRANSFER_CANCELLED` that the void and cancel flows would read as their own.
- The layer is then drained to zero by one `STOCK_REVISION_ZEROING` draft (`StockEntryDrainer`) carrying what the layer held
  plus what the consumers gave back, cost included, so the drain is not a write-off. Its document reference is the stock
  revision's reference and its detail reference is the layer's own (the replaced delivery line's). It is recorded in the
  product's storage unit, because a layer keeps no unit of its own. A zero-quantity layer has nothing to drain and gets no draft.
- The restore drafts carry the context "Restored for stock revision <reference>" and the drain "Drained by stock revision
  <reference>, which revised delivery <delivery>/<line>" (`StockMovementContexts`; "delivery" only for a purchase layer).
  Every draft carries the revision's reference, stored in `stock_movement.stock_revision_reference`, so one query finds
  everything a revision wrote.
- The product balance on each draft is the running balance, so the drain leaves it short until the replacement layer exists.
- A consumer whose type is in `MovementType.movementsThatCannotBeReversed` is rejected before anything is written, all
  together in one `BlockedChangeException`, one `BlockingProblem` per consumer. Only `SUPPLIER_RETURN` can reach this check, because a
  consumer is always a decrease. `CUSTOMER_RETURN` increases stock, so it is never a consumer; a layer holding one is
  refused before a revision starts, by `DeliveryLineStockInspector.hasMovementsThatCannotBeReversed`.
- Only `SALE` and `STOCK_ADJUSTMENT_DECREASE` consumers can be replayed, because only they have a ledger restatement
  (`RestatableConsumers`). The assessor turns any other consumer on the layer into a `BlockingProblem`, so the preview shows it,
  and the revisioner checks the same set again before it writes, so a type added later fails loudly instead of leaving the
  ledger's inventory behind the stock. Transfers and supplier returns are refused earlier, by the delivery validator.

## The replacement layer (`StockReplacementLayerRecorder`)

Recorded through `StockReplacementLayerRecorder` (in `stock/api`): it keeps the original delivery's document reference, takes
the new line's reference as detail reference, and the caller supplies source type, movement type and the id of the revised layer.
The replacement takes the revised layer's `activatedOn` and keeps priority 0, so it sorts into the place the revised layer held in
the draw order, however much later it is recorded. Replacements made weeks apart, even for the same product, each keep their own
place, and no counter or priority band is involved. `createdOn` still says when the row was written; `activatedOn` is a place in the
draw order, not an insert time.
Because the replacement sits where the revised layer sat, a replay draws any older layer that still holds stock before it, as the
draw order would have. Its movement context names the stock revision (`StockMovementContexts`).

## Planning the replay (`ReplayPlanner`)

The consumers of the replaced layer still need their stock, so `ReplayPlanner.planReplayForExecution(consumptions, stockRevisionReferenceNumber, quantityBreakdown)`
plans their draws against the layers as they stand, which must already include the replacement. It reads the layers in draw order
(`StockEntryFetcher`) and the product's name, and hands them, with the breakdown it was given, to `ConsumerReplayer`, which plans every consumer on
`StockEntryFifoDto` copies of the layers and writes nothing. It returns one `ConsumerReplayPlan` per consumer (the draw to
make and the layers to take from) and collects every uncovered consumer as a `BlockingProblem` in `ReplayResult.problems`; `planReplayForExecution` throws them as one
`BlockedChangeException`. A replacement smaller than what was drawn is normal: the rest of each consumer's quantity comes
from the next layers in order, and only when all layers together cannot cover a consumer does the replay fail.

- The replayer takes the consumptions of one replaced layer and rejects consumptions of several, since a revision replaces one
  layer at a time. Each consumption replays as one draw of its net quantity, oldest draw first, by the creation time (then id)
  of its first draw on any layer, whatever order the caller lists them in. A consumer that also drew from other layers keeps that part
  as it was, and a later revision of one of those layers replays its portion there.
- A replayed draw keeps the draw's type, document and detail references, whole-line quantity text and adjustment
  category, reason and note, carries the context "Replayed after stock revision <reference>", and stores the revision's
  reference in `stock_movement.stock_revision_reference`, so it does not collide with the document's earlier draw on the same layer.
- Each plan reports `oldCost` (the net cost put back on the replaced layer) and `newCost` (the sum of its takes). They cover only what was drawn from the replaced
  layer: a consumer that also drew from other layers keeps that part as it was.

## Assessing a revision (`DeliveryLineRevisionAssessor`)

`DeliveryLineRevisionAssessor.assess(request)` finds every blocking problem without writing anything: the line checks
(`PurchaseDeliveryLineReplacer.validateRevisionReportingProblems`), the replay against the layers as they will stand after the
revision, and the reservation check. The save calls it under the purchase and product locks and throws the problems as one
`BlockedChangeException` before the first write, so a blocked save writes nothing and draws no reference numbers.
`PurchaseDeliveryLineReplacer.replace` takes the `ValidatedDeliveryLineRevisionDto` the assessment produced and refuses one that
still carries problems. The preview calls the same assessor without locks and turns the result into rows.

## Previewing a revision (`DeliveryLineRevisionPreviewer`)

`DeliveryLineRevisionPreviewer.preview(request)` returns a `DeliveryLineRevisionPreviewDto` and writes nothing. It takes no
lock, so it reports what the save would plan at that moment; the save plans again under the product lock, and nothing is cached
between the two. It runs the same checks as the save (`PurchaseDeliveryLineReplacer.validateRevisionReportingProblems`), reads the consumptions of
the revised layer (`NetConsumptionCalculator`), and replays them with `ReplayPlanner.planReplayForPreview` over the layers as they will
stand after the revision: the revised layer removed and the replacement added by `StockReplacementLayerPreviewer`. The replacement
comes from the same builders the save uses (`StockReplacementLayerSpecs`, `DeliveryReplacementLayerRequests`), so it sorts into
the draw order where the save will put it. `ResetPlanner` is not run, since it only plans the writes.

- `ReplayResult.problems` holds one `BlockingProblem` per consumer the later stock cannot cover. The preview shows each as
  a blocking row; the save throws them as one `BlockedChangeException`. The validator's problems take the same two routes.
- The request carries the unit of the new quantity and cost, which may differ from the revised line's. The replacement is
  converted with its own unit's factor and compared with the revised line using the revised line's factor
  (`ValidatedDeliveryLineRevisionDto.storageQuantityChange`), and the change summary names each side's unit.
- `RevisionPreviewStoryWriter` writes every sentence of the response: the header, each item's reference, description and effect,
  and the money and quantity text, using the product's `QuantityBreakdown` and `DisplayFormatters`. The screen only places the text.
  A consumer whose cost does not move gets no item. `netProfitChange` is set for sales only (old cost minus new cost).
- A consumer the stock cannot cover takes what is left, so later consumers are planned against the stock that remains and
  each shortfall counts once (the uncovered consumers' own quantities add up in `ReplayResult.totalTakenByUncoveredConsumers`). Its row states how much
  of it could still be covered, counting every layer, and gives no quantity to type, since a number offered for the purpose of passing the check invites a wrong value. While anything
  blocks the save the response carries the blocking rows only, because the cost rows would be wrong.
- `RevisionGuidanceWriter` writes the `guidance` paragraphs for the "what can I do" tab as `TextSegmentDto(text, emphasis)`
  lists, so the screen places and styles them and holds no wording. For uncovered consumers it names their references, the
  quantity they took from this delivery between them (what the records show, not a target) and the quantity the change leaves, then always lists the possible causes: the delivery
  was entered short, a sale was recorded wrongly (correct or void it), or the stock came from a delivery that was never
  recorded. A revision to 0 adds that the sales must be voided first. The paragraphs are written for sales, so they are left out when any uncovered consumer is a transfer, stock adjustment or supplier return (`ReplayResult.movementTypesOfUncoveredConsumers`); its rows still show. For draft sales it names the drafts and says nothing
  has been sold on them. Product names in the rows are the product name alone, not the full label.
- Not yet reported: draft transfers, a payment above the revised total, and
  ledger entries (`ledgerEntries` is empty until revisions post to the ledger).

## Transactions

`DeliveryLineRevisioner` runs the whole revision in one transaction. Every write inside it (`StockRevisionCommitter.commitReset`
and `commitReplay`, `StockReplacementLayerRecorder.record`, `PurchaseDeliveryLineReplacer.replace`,
`PurchaseDeliveryRevisionApplier.apply`) is `Propagation.MANDATORY`, so none of them can commit part of a revision when called
alone. Spring enforces this at runtime; `RevisionWritePathPropagationTest` pins the annotation on each method. The preview
(`DeliveryLineRevisionPreviewer`) is read-only and takes no lock.

## Ledger (`StockRevisionHandlerForKafka`)

After the revision's stock rows are written, `DeliveryLineRevisioner` publishes a `PurchaseDeliveryRevisedEvent` carrying the
revised and replacement line totals (`ReplacementDeliveryLineDto`, from `PurchaseDeliveryLineEntity.lineTotal`). The event binds to
the transaction commit, and an unchanged total publishes nothing, since the ledger rejects zero entries.
`PurchaseDeliveryRevisionAccountingProcessor` posts the change as `PURCHASE_DELIVERY_REVISION`, keyed on the stock revision
reference: Dr Inventory and Cr Trade payables for an increase, the opposite for a decrease. The supplier subledger holds no
negative amounts, so a decrease is recorded on the receivable side, as a supplier payment is. The posting date is the
revision date, in the current period. A reissue rebuilds the event from the stock revision, the revised line (the detail
reference of the revised stock entry) and the replacement line.

Sale costs restate in the same transaction. `ConsumerCostChange` holds each consumer's cost over the part it drew from the
revised layer, built from the replay plans on save and from the revision's tagged movements on a reissue (the restore
movements carry the old cost, the replayed draws the new). `SaleCostRestater` (`sale/api`) replaces each affected sale
line's active `sale_line_cogs` row with one that moves by the difference, leaving the sale itself unchanged. Every draw's cost is already at money scale, so the totals are exact and no rounding step exists: a finer value fails in
`Money.of`. The totals publish a `SaleCostRestatedEvent`, and `SaleCogsRestatementAccountingProcessor` posts the net as `SALE_COGS_RESTATEMENT`, keyed
on the stock revision reference: Dr Cost of goods sold and Cr Inventory when cost rises, the opposite when it falls. The
posting is in the current period and counts in the daily profit and loss.

Stock adjustments that drew from the revised layer restate in the ledger only. The adjustment record is immutable and keeps its
original `total_cost`; the replayed movements and the ledger carry the restated cost. `StockAdjustmentRestatementHandlerForKafka`
totals the old and new cost of each restated adjustment and publishes a `StockAdjustmentCostRestatedEvent`.
`StockAdjustmentRestatementAccountingProcessor` posts `STOCK_ADJUSTMENT_RESTATEMENT`, keyed on the stock revision reference:
each adjustment's own legs for an increase, reversed for a decrease, netted by account. The accounts are the ones the original
`STOCK_ADJUSTMENT` posting used (`PostedStockAdjustmentAccountsReader`), so a later change to the category rules never splits an
adjustment across two sets of accounts. The event also carries the accounts the current rules give
(`StockAdjustmentAccountCodesFetcher`), used only for an adjustment whose original cost was zero, which posted no entries. If the
original posting does not exist yet, the processor fails, the event is logged as FAILED and the event log sweeper retries it
every 5 minutes. A reissue rebuilds both restatement events from the movements tagged with the revision.

Example: an internal-use adjustment restates from 5.00 to 4.50 and a shrinkage adjustment from 2.00 to 2.30. The first debits
Internal use expense by -0.50 and credits Inventory by -0.50; the second debits Shrinkage by 0.30 and credits Inventory by 0.30.
Netted by account, the posting is Cr Internal use expense 0.50, Dr Inventory 0.20 and Dr Shrinkage 0.30.

## Reservations (`ReservationShortfallChecker`)

A draft sale holds stock without drawing from a layer, so the replay never sees it. A revision that lowers on-hand stock can
still leave the draft sales holding more than is left. `ReservationShortfallChecker` compares the product's reserved total
(`StockReserver`) with its on-hand quantity after the revision. If the reservations exceed it, there is one `BlockingProblem`
per draft sale holding the product (reference with "(draft)", the quantity it holds), all with the same effect line: how much is
in stock, how much the draft sales hold, and to remove the product from a draft sale. The check is per product and does not say
which sale loses, since any of them could.

- The preview adds the on-hand change the revision will make (`ValidatedDeliveryLineRevisionDto.storageQuantityChange`, replacement
  minus revised quantity) to the current balance. Every consumer is replayed in full, so that is the whole change. The check is
  skipped while a replay shortfall already blocks the save, because the arithmetic assumes every consumer is covered.
- The save runs the same check as part of the assessment below, before any write.
- Draft transfers hold no reservation and are not checked.

## Blocking problems (`BlockingProblem`, `BlockedChangeException`)

Anything that stops a revision for a reason the person can act on is a `BlockingProblem(documentReference, description,
effect)`, written in plain shop language: what is wrong, then what to do. The save throws them as one `BlockedChangeException`
whose `message` is a single headline ("This change cannot be saved. 2 things need fixing.") and whose payload is the problem
list, so the screen reads `error.error.body` as items and shows the headline from `message`. The preview returns the same
problems as `BLOCKS_SAVE` rows, so one rendering serves both and a save that meets a new blocker after a preview shows it
the same way. A problem that only support can fix uses `BlockedChangeException.contactSupport`, whose headline says so and whose
rows quote the stock record reference. A single-sentence failure with nothing to list (a line that cannot be found, a missing
reason) stays a plain `RtsGenericException`.

## The stock revision row

`StockRevisionEntity` (`stock_revision` table) links the revised layer to its replacement under the revision's reference, with the
source type (`DELIVERY_LINE`), the replacement delivery line's reference and the reason. The reference is drawn before the reset
because every movement names it; the row is saved last, once the replacement layer exists.

## Committing a revision (`StockRevisionCommitter`)

`StockRevisionCommitter` (`stock/api`) writes the reset and the replay of a revision; the replacement layer between them is written by
`StockReplacementLayerRecorder`. It joins the caller's transaction and does not enforce one. It writes plans that were made under the
product lock `DeliveryLineRevisioner` holds, so it does not validate them again; a layer that is already `REPLACED` is refused earlier, by
`ResetPlanner`.

- `commitReset(StockEntryResetPlan, quantityBreakdown)` takes the product lock before it reads the layer (locking again in the same
  transaction is harmless). It builds a movement from each `StockMovementDraft` (`StockMovementCreator`, adding the layer and the
  quantity breakdown text), sets the layer's remaining quantity and cost to zero, marks it `REPLACED` and saves both.
  A zero-quantity layer is marked `REPLACED` too, with no movements.
- `commitReplay(plans, replacementStockEntryId, quantityBreakdown)` draws every plan against the layers with stock left, in plan
  order, with `FifoStockConsumer.applyTakes`, then saves the touched layers and the movements. It refuses a replacement that is missing,
  already replaced or of another product (the reason is on the check).
