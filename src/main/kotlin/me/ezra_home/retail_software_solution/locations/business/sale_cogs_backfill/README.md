# Sale COGS Backfill — Rules & Expectations

One-off data fix that copies the legacy `sale_line.cogs_total` into `sale_line_cogs` rows. It owns no tables.

## Entry point

- `SaleLineCogsBackfiller.startBackfillAllLocations()` (`api/`), reached through
  `POST secured/admin-data/sale-line-cogs/backfill` (organization admin, no location header needed). The endpoint
  answers 204 and the work runs asynchronously under the record-initializer service account.
- It walks every location of the organization and switches the session to each one in turn.

## What gets copied

- A `sale_line_cogs` row (`ACTIVE`) for every sale line with a non-null `cogs_total` and no row, voided sales
  included. Draft lines have no cost and are skipped.
- The cost is the column's value as is, `created_on` is the sale's `date_sold`, and the reference number comes from
  the table's sequence.
- The line is the key, so a partly copied sale gets its remaining lines and a re-run only fills what is missing.

## Transactions and failures

- `LocationSaleLineCogsBackfiller.backfillNextBatch` is one transaction of up to 500 lines, with reference numbers
  drawn in bulk, and the orchestrator loops until a batch comes back empty.
- A failing location is logged with its error and skipped. Batches it already committed are kept, and the other
  locations still run.
- Sale internals are reached only through `SaleLineCogsBackfillStore` in `sale/api`.

## Reading the result

Completion is read from the application log: one block per location (inserted this run, lines with cost, cost
records, reconciled) and a final summary listing each location as `OK`, `MISMATCH` (the two counts differ) or
`FAILED`. A location is safe to reopen only when its counts match.
