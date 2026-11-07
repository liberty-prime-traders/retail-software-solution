# Product Purchases Report

A read-only, per-product report of what was received from suppliers and what it cost. Nothing is stored and no
tables are added. Each product row can be expanded into the delivery lines that make up its figures.

This README is the rules document for the report. It states what the code does and what it guarantees.

## Endpoints

| Method | Path                                                                  | Returns                                         |
|--------|-----------------------------------------------------------------------|-------------------------------------------------|
| POST   | `secured/product-purchases-report/summary`                            | `List<ProductPurchasesReportRowDto>`            |
| POST   | `secured/product-purchases-report/{locationProductId}/delivery-lines` | `PageResponse<ProductPurchasesDeliveryLineDto>` |

## Status rules

Two status columns decide what is counted. `ProductPurchasesReportQueryBuilder` applies both.

- **Delivery header** (`purchase_delivery.status`): only deliveries where `PurchaseDeliveryStatus.isAppliedToStock()` is
  true count: `RECEIVED`. `PROCESSING` and `FAILED` are excluded. This filter
  applies to both the summary and the expansion (`buildReceivedDeliveryPredicate`).
- **Delivery line** (`purchase_delivery_line.status`): `ACTIVE` lines count toward summary totals only
  (`buildPredicate`). `REVISED` lines stay visible in the expansion and are excluded from totals, so a corrected line
  is counted once, through its correction delivery. A line becomes `REVISED` only through a delivery revision.

## Counting rules

- The date filter is `purchase_delivery.delivered_at`, not the purchase date. Both bounds are required, and the span
  is bounded by the shared `SearchGuards.guardBoundedRange` guard.
- Location scope comes from the session, never from the request.
- Filters: `locationProductIds`, `categoryIds` (flat match on `location_product.category_id`), and `supplierIds`.
- `deliveryLineCount`, storage quantity and `totalCost` are summed in SQL. Each line's total is rounded to money scale
  before it is summed, so a product total equals the sum of the line totals that the expansion shows.
- `averageCostPerBaseUnit` is the weighted average: total cost divided by delivered storage quantity, expressed per
  base unit. It is returned as currency-formatted text, through `DisplayFormatters.formatCurrency`. It is null when no
  storage quantity was delivered.
- `receivedQuantityBreakdown` is the summed storage quantity rendered by `QuantityBreakdown.render`, split into whole
  counts per unit level, for example "2 Dozens 1 Piece". Only the smallest unit can carry a fraction.

## Expansion

- Lines are keyed by the delivery line id and paged by `(delivered_at, id)` descending, newest first.
- Each line's quantity text is rendered with the unit it was entered in. Its base quantity uses the ratio frozen on the line.

## Live, not a snapshot

Every request recomputes from the current data. A future restatement changes past averages, and that is accepted.

## Keys

- Summary rows are keyed by `location_product_id`. Expansion rows are keyed by the delivery line id.
- The keys stay stable because no flow edits an existing delivery line. A revision must therefore create new lines
  rather than edit the originals. If a flow is added that edits a line in place, these keys no longer identify a
  single figure and the expansion must be reworked first.
- The status predicates live in `ProductPurchasesReportQueryBuilder`, so the summary and the expansion always apply
  the same header filter.
