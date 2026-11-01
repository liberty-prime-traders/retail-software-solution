# Product Sales Report Package: Rules & Expectations

Read-only reporting over confirmed sales, per product. Nothing is stored and no tables are owned: it reads
`sale`, `sale_line` and `sale_adjustment` with native SQL by table name, so it never imports a `sale` or
`sale_adjustment` class beyond `sale/api`'s `SaleStatus`. Public surface is `api/`:
`ProductSalesReportService` (lines + summary), `ProductSalesReportSearchService` (the searchable, sortable report over a
date range) and their DTOs.
REST: `ProductSalesReportEndpoint` under `secured/sales/product-report`.

## Product sales report: `ProductSalesReportService.searchLines(PageRequest<ProductSalesLinesParameters, String>)` / `.summarize(ProductSalesSummaryParameters)`
- Nothing is stored. Native SQL in this package; `ProductSalesReportCalculator` derives
  everything (net, profit, margins, per-base-unit figures) in Kotlin from the summed totals.
- Sources: `sale_line.line_total` (gross, never modified by adjustments), `sale_line.cogs_total`
  (null counts as zero, as in `SaleDataFetcher.getSaleCogsTotal`) and line-level `sale_adjustment` rows
  (`sale_line_id` not null; order-level adjustments are ignored). Status is fixed to `CONFIRMED`.
- `from` is inclusive and `before` exclusive on `sale.date_sold`, both `OffsetDateTime`, required, at most
  one year apart (`SearchGuards.guardBoundedRange`). Lines page by `(date_sold, sale_line.id)` descending;
  the cursor's `createdOn` slot carries `date_sold`. The range predicate excludes null `date_sold`.
- Summary SQL aggregates adjustments in a `LEFT JOIN LATERAL` that yields one row per line, so a line
  with several adjustments is never counted more than once in gross, cogs or quantity.
- Stored adjustment amounts are unsigned magnitudes; responses sign them (`DISCOUNT` negative,
  `SURCHARGE` positive): `netSales = grossSales + discountTotal + surchargeTotal`. A price override is an
  ordinary adjustment and is not separated by reason.
- Quantity is summed in storage units from each line's frozen `storage_units_per_user_provided_unit`
  and converted once per product with the base unit's current size (`UnitConversionGraph.getSizeInRoot`).
  A unit resized after a sale changes how that quantity reads in base units, never the stored quantity.
  Line rows convert per line for display only.
- `baseQuantityText` is the base quantity rendered by `QuantityBreakdown` ("1 Piece", "24 Pieces"), the same renderer
  stock movement history uses, so the base unit is pluralized in one place.
- Money is scale 4 and percentages scale 1, half-up through `Decimals`. A percentage is null when its
  denominator is zero; per-base-unit figures are null when the base quantity is zero.
- `quantityEntered` is the line's own unit as typed ("2.5 Bales"), via
  `QuantityBreakdown.renderUserProvided`, the same renderer stock movement history freezes.
- Like sale search, the endpoints carry no `@PreAuthorize`; only `/secured/**` authentication applies.
- REST on `ProductSalesReportEndpoint`: `POST /secured/sales/product-report/{locationProductId}/lines` (the product is
  the path key, so a keyed paged client can cache pages per product) and `POST /secured/sales/product-report/summary`. Summary requires 1 to 4 distinct `locationProductIds` and returns them in request order,
  with zeros for a product that sold nothing.
- Every summary is built by `ProductSalesSummaryAssembler` (identity from
  `LocationProductDataFetcher.findSummaryByIds`, a narrow column projection: `orgProductId`, `productName`,
  `productGroupName`, base unit), so the summary endpoint and the search below cannot disagree.
  `adjustmentsPercent` is the discount rate: `(grossSales - netSales) / grossSales`.

## Product sales report search: `ProductSalesReportSearchService.search(PageRequest<ProductSalesReportSearchParameters, String>)`
- Lists every product with at least one confirmed sale line in the requested range; a product without sales never
  appears. Parameters: `from`/`to` (required `LocalDate`s), `searchText`, `sortField` (default `NEEDS_ATTENTION`) and
  `sortDirection` (default `DESC`). The UI's default range is today to today; the server assumes no default and no
  fixed length.
- `from` and `to` are organization-zone calendar days, both inclusive, with `from` not after `to` and less than one year
  apart (`ProductSalesReportWindow.ofInclusiveDates`). Internally the window is `from` to the day after `to`, so the
  day buckets combine into any range. The lines and summary endpoints keep `OffsetDateTime` bounds.
- Day buckets (`ProductSalesDayBucketCache`): in-process Caffeine, not Redis and not a `@Cacheable` (the global spec
  has no expiry). One bucket per location schema and organization-zone calendar day, holding each selling product's
  additive totals (`ProductSalesSummaryRawRow`: line count, storage quantity, gross, cogs, discount and surcharge
  magnitudes). Expires 15 minutes after write; reads never extend it. It is not evicted on sale confirm or void, so the TTL
  bounds how stale a figure can be, including today's partial day. Bounded by weight, one per product row, maximum
  200,000. The cache is per node.
- Missing days load in one grouped query per run of consecutive missing days
  (`ProductSalesReportQueryBuilder.buildDailySummaryQuery`, grouping by the `date_sold` calendar day in the
  organization zone and the product), so cached days between two gaps are never aggregated again. The query has a
  30-second timeout instead of the 2-second per-request one because the cache absorbs the cost.
- Everything derived is recomputed from the summed buckets, never added across days: net, profit, margins, discount
  rate, per-base-unit figures, `baseQuantityText` and rank. Only additive totals are bucketed, so no distinct count
  may be added to `ProductSalesSummaryRawRow` without breaking this.
- `ProductSalesReportSnapshotCache` holds the combined rows for a range: Caffeine, expires 1 minute after write,
  maximum 20 entries, key = location schema + first day + day after the last day. Its only job is to avoid re-assembling
  product identities (`ProductSalesSummaryAssembler`) on every page, so it can lag the buckets by at most its own TTL.
  `ProductSalesReportSnapshotBuilder` is the only transactional step, so a request served from the cache opens no
  connection. `ProductSalesReportSnapshot.builtAt` is the age of the oldest bucket behind the rows, and is what
  `ProductSalesReportSearchMetadata.snapshotBuiltAt` reports (organization-zone `OffsetDateTime`); it is the same on
  every page served from one snapshot, and the response is a `PageResponseWithMetadata`, the same fields as `PageResponse` plus a typed `metadata`.
- Orders (`ProductSalesReportOrdering`). `NEEDS_ATTENTION`, descending only: tier 1 gross profit below zero, largest
  loss first; tier 2 the rest that are net discounted (`grossSales - netSales > 0`: discounts exceed surcharges, so equal
  discount and surcharge is not net discounted), most net discount money first; tier 3 the rest by net sales descending.
  Every other `ProductSalesSortField` sorts by that field in either direction. A null value (a percentage whose
  denominator is zero) sorts last in both directions, and ties break on lowercase product name, then id, ascending.
  Each order is computed once per snapshot (`ProductSalesReportSnapshot.orderedRows`) and a cursor is located in it by
  binary search (`rowsAfter`).
- `ProductSalesSummaryDto.rank` is the 1-based `NEEDS_ATTENTION` position across the whole range, assigned when the
  snapshot is built. A sort, a search or a later page never renumbers it. It is null on the summary endpoint. A rebuilt
  snapshot can rank the same product differently.
- Threshold mode: `rts.product-search.client-side-filter-threshold` (shared with product search, inclusive) is compared
  against the number of products with sales in the range. At or below it, and on a request with no cursor, every row
  comes back in the requested order with `requireClientSideFilter = true`, an empty cursor, and `searchText` and
  `requestedSize` ignored. Above it, the service filters `searchText` (contains on product name or group, ignoring case
  and all whitespace via `StringUtils.normalizeForComparison`; the comparison forms are computed once per snapshot) and
  pages the ordered list in memory; page size is 1 to 500.
- Cursor (`ProductSalesReportCursor`): the last row's full ordering key (tier, number, text, lowercase name, id) plus the
  sort field and direction. The key is a position in that order, applied to the filtered list, so a page continues
  correctly under a search and after a snapshot rebuild, even if the last row is gone. A cursor only continues the
  sort it was issued for; a different `sortField` or `sortDirection`, a malformed cursor or a tampered one is rejected.
  The range is not in the cursor: the client repeats it on every page request.
- `NEEDS_ATTENTION` with `ASC` is rejected (`ProductSalesReportValidator.guardValidSort`).
- There is no high-margin flag: the UI marks a margin above 10 itself.
- REST: `POST /secured/sales/product-report/search` on `ProductSalesReportEndpoint`; no `@PreAuthorize`, like the
  other report endpoints.
