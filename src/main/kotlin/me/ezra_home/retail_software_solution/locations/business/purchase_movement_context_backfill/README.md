# Purchase Movement Context Backfill — Rules & Expectations

One-off data fix that fills `stock_movement.context` on `PURCHASE_RECEIVED` movements recorded before deliveries wrote
it. It owns no tables.

## Entry point

- `PurchaseMovementContextBackfiller.startBackfillAllLocations()` (`api/`), reached through
  `POST secured/admin-data/purchase-movement-context/backfill` (organization admin, no location header needed). The
  endpoint answers 204 and the work runs asynchronously under the record-initializer service account.
- It walks every location of the organization and switches the session to each one in turn.

## What gets updated

- A `PURCHASE_RECEIVED` movement whose `context` is empty and whose document reference is a delivery's reference gets
  `StockMovementContexts.purchase(<the purchase's reference>)`, the same text new deliveries write.
- The purchase is found through the delivery (`purchase_delivery.reference_number` = the movement's document
  reference). A movement with no matching delivery is not a candidate and keeps an empty context.
- A movement that already has a context is left alone, so a re-run only fills what is missing.

## Transactions and failures

- `LocationPurchaseMovementContextBackfiller.backfillNextBatch` is one transaction of up to 500 movements, paged by
  movement id, and the orchestrator loops with the last id of each batch until one comes back empty.
- Movements are saved as entities, so the audit table records each change.
- A failing location is logged with its error and skipped. Batches it already committed are kept, and the other
  locations still run.
- Stock internals are reached only through `PurchaseMovementContextBackfillStore` in `stock/api`.

## Reading the result

Completion is read from the application log: one block per location (updated this run, still missing) and a final
summary listing each location as `OK`, `INCOMPLETE` (movements still missing a context) or `FAILED`.

## Lifetime

The package, its store in `stock/api`, the two native queries on `StockMovementRepository` and the endpoint are
removed once every organization has run the backfill, as with the sale COGS backfill.
