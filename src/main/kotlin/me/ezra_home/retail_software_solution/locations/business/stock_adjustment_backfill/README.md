# Stock Adjustment Backfill — Rules & Expectations

One-off data fix that gives adjustments recorded before `stock_adjustment_cost` existed their `ACTIVE` cost row. It owns no tables. The quantity text is written when the adjustment is recorded and is never touched by this job.

## Entry point

- `StockAdjustmentBackfiller.startBackfillAllLocations()` (`api/`), reached through
  `POST secured/admin-data/stock-adjustment/backfill` (organization admin, no location header needed). The endpoint
  answers 204 and the work runs asynchronously under the record-initializer service account.
- It walks every location of the organization and switches the session to each one in turn.
- A start while a run is in progress is refused with an error. The flag lives in the process, so it guards one application
  instance; across instances the partial unique index on `ACTIVE` rows keeps an overlap harmless.

## What gets copied

- An `ACTIVE` `stock_adjustment_cost` row for every adjustment without one, with the cost from `stock_adjustment.total_cost`,
  `created_on` from the adjustment, and a reference number from the table's sequence.
- The adjustment is the key, so a re-run only fills what is missing.

## Transactions and failures

- `LocationStockAdjustmentBackfiller` runs one transaction per batch of up to 500 adjustments, and the orchestrator loops until a
  batch comes back empty.
- A failing location is logged with its error and skipped. Batches it already committed are kept, and the other
  locations still run.
- Stock adjustment internals are reached only through `StockAdjustmentBackfillStore` in `stock_adjustment/api`.

## Reading the result

Completion is read from the application log: one block per location (cost records inserted, adjustments, active cost
records, reconciled) and a final summary listing each location as `OK`, `MISMATCH` (adjustments and active cost records
differ) or `FAILED`. A location is safe to reopen only when its counts match.
