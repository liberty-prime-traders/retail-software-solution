# Last Purchase Cost Backfill — Rules & Expectations

One-off data fix that rewrites `location_product.last_purchase_price` as the cost per base unit. Values written before
`LocationProductService.updateLastPurchaseCosts` converted them hold the unit cost in whichever unit the delivery line
was typed. It owns no tables.

## Entry point

- `LastPurchaseCostBackfiller.startBackfillAllLocations()` (`api/`), reached through
  `POST secured/admin-data/last-purchase-cost/backfill` (organization admin, no location header needed). The endpoint
  answers 204 and the work runs asynchronously under the record-initializer service account.
- It walks every location of the organization and switches the session to each one in turn.

## What gets updated

- The source is the latest delivered cost of each location product: among `ACTIVE` lines of `RECEIVED` deliveries, the
  line on the delivery with the greatest `delivered_at` (ties broken by delivery id, then line id). The purchase line
  supplies the location product.
- The price is converted by `LocationProductService.updateLastPurchaseCosts`, the same code new deliveries use, so the
  backfill and live writes cannot disagree.
- A product with no received delivery line keeps its price.
- The price is recomputed from delivery lines, never from the stored value, so a re-run rewrites the same numbers and
  is safe to repeat.

## Transactions and failures

- `LocationLastPurchaseCostBackfiller.backfillNextBatch` is one transaction of up to 500 products, paged by location
  product id; the orchestrator loops with the last id of each batch until one comes back empty.
- Products are saved as entities, so the audit table records each change.
- A failing location is logged with its error and skipped. Batches it already committed are kept, and the other
  locations still run.
- Delivery internals are reached only through `LastPurchaseCostBackfillStore` in `delivery/api`.

## Reading the result

Completion is read from the application log: one block per location (updated this run) and a final summary listing each
location as `OK` or `FAILED`.

## Lifetime

The package, its store in `delivery/api`, the native query on `PurchaseDeliveryLineRepository`, its projection and the
endpoint are removed once every organization has run the backfill, as with the purchase movement context backfill.
