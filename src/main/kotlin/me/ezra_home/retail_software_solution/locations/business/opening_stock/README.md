# opening_stock

Lets a location declare the starting quantity/cost for a product's stock
when it is first set up in the system. `opening_stock` rows are
**declare-once, immutable** — one row per `locationProductId`
(`uq_opening_stock_location_product_id`), never corrected in place and
never re-declared. There is no "re-declare" flow.

This mirrors `organizations/business/opening_balance`'s shape (an
append-only declaration entity that fans out into a Kafka event for
ledger posting), adapted to the fact that a location's stock already has
its own durable ledger — `stock/StockEntryEntity` /
`StockMovementEntity` — so there's no need for opening_stock itself to be
correctable or queried for "current" state; that lives in `stock/` like
every other movement.

## Fixing a wrong declaration

If an opening stock declaration turns out to be wrong, it is **not**
corrected here. It's fixed the same way any other stock error is: a
stock adjustment (`stock_adjustment/`, `MovementType.STOCK_ADJUSTMENT_INCREASE` /
`STOCK_ADJUSTMENT_DECREASE`). An adjustment lot is created with `priority = 0`, the way
`PurchaseDeliveryStockUpdater` and `OpeningStockStockUpdater` do, so it is ordered by creation time rather than
relative to the original opening-stock lot. Making `opening_stock` itself correctable was tried and reverted
— it required drawing down/topping up the original lot via signed
deltas with no real way to express priority for the correction, which is
exactly the adjustment feature's job, not this one's.

## Why a separate table at all

`StockEntryEntity`/`StockMovementEntity` already record the quantity and
cost. `OpeningStockEntity` exists alongside them purely to:

- Anchor the "already declared for this product" guard
  (`OpeningStockRepository.findByLocationProductIdIn`, backed by the
  unique constraint) — the one thing `stock/` has no natural row for.
- Carry the declaration's **unit**: `opening_stock` stores `user_provided_quantity`, `unit_id` and
  `storage_units_per_user_provided_unit` (the unit's size in the product's storage unit, captured at declaration, which also rejects a
  unit outside the product's unit group) beside `unit_cost` (per `unit_id`). `OpeningStockAmountsFetcher` returns the
  unit with the quantity.
- Carry its own `HasReferenceEntity` reference number, which becomes both
  `StockEntryEntity.documentReference` (idempotency, with a null `detailReference`: an opening stock has no line
  reference) and the Kafka event's `sourceReferenceNumber` for ledger posting / DLT
  reissue.

## Flow (`OpeningStockService.declareInitialStock`)

1. Guard: non-empty lines, no duplicate products in the request
   (`OpeningStockValidator`).
2. `fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())`
   — today, org-zoned; there is no historical "as of" date for an initial
   declaration, unlike a purchase delivery's `deliveredAt`.
3. `entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)` — the
   same namespace `StockAvailabilityValidator` locks before reading a
   balance, so a concurrent sale/delivery/declare on the same product
   serializes here too. This runs **before** the reads below, not after
   — `guardAllActive` and the "already declared" check both need to see
   a state that can't shift under them between the read and the lock.
4. Resolve product labels (`locationProductDataFetcher.findSummaryByIds`,
   `LocationProductSummaryDto.label`) up front — every guard from here on
   reports the human-readable label, not a bare `locationProductId`, in
   its failure message.
5. Quantity (`OpeningStockLineDto` holds it as a `PositiveUserProvidedQuantity`, so positive with at most 2 decimals) / unit cost per line (a `PositiveMoney`, so positive and at most the money scale in decimals, checked when the request binds), then
   `locationProductService.guardAllActive` — same guard used at
   purchase-creation time.
6. Guard none of the requested products already have an `opening_stock`
   row. Two concurrent declarations for one product leave one: the second fails on
   `uq_opening_stock_location_product_id`.
7. Persist the `OpeningStockEntity` rows in one `saveAll` (not a
   per-line `save` in a loop), storing the typed quantity, its unit, the captured
   `storage_units_per_user_provided_unit` and the unit cost exactly as declared.
8. Call `stock/api/OpeningStockStockUpdater.recordOpeningStock` — the
   only cross-domain call this service makes into `stock/`, since
   `ArchitectureTest` forbids reaching past its `api/` package.
   It persists a `StockEntryEntity` (source `StockItemSource.OPENING_STOCK`)
   + `StockMovementEntity` (`MovementType.OPENING_STOCK`) per line,
   **synchronously, in the same location transaction** — unlike a
   purchase delivery, there's no upstream domain to decouple from and no
   multi-step status machine to protect, so this doesn't need
   `InventoryEventProcessor`'s async hop. The `OpeningStockEntity`'s
   reference number is what ties the two together
   (`StockEntryEntity.documentReference`). The layer's `batch_cost` is the declaration's line total
   (`unitCost × userProvidedQuantity`, rounded once to cents) and its storage quantity is exact.
9. Publish one `OpeningStockDeclaredEvent` per line via
   `OpeningStockHandlerForKafka.publish`; it carries the line total as `Money`, computed once there, so an oversized
   total fails inside the declaring transaction. Ledger posting still has to be
   async — it crosses into the organization datasource — handled by
   `ledger/processors/OpeningStockAccountingProcessor`
   (`SystemAccount.INVENTORY` debit / `SystemAccount.OPENING_BALANCE_EQUITY`
   credit, `LedgerSourceType.OPENING_STOCK`), the same offsetting account
   `OpeningBalanceAccountingProcessor` uses for org opening balances.

## REST surface

`OpeningStockEndpoint` (`secured/opening-stock`):

- `POST` (body: `lines: [{locationProductId, quantity, unitId, unitCost}]`)
  → `OpeningStockService.declareInitialStock`. Response is
  `List<OpeningStockResponseDto>` — `referenceNumber`, `locationProductId`,
  `productLabel`, `quantity`, `unitId`, `unitCost`, `declaredBy`, `declaredAt`.
- `GET` → `OpeningStockService.getAll` (all declarations for the current
  location, used by the UI to grey out products that already have an
  opening stock).

Both are thin delegators; all validation/locking/posting logic lives in
`OpeningStockService`.

## Rollup onto `LocationProductResponseDto`

`location_product`'s `LocationProductEnricher` surfaces
`openingStockQuantity`/`openingStockUnitCost` (both nullable — absent
means never declared) on every `LocationProductResponseDto`, the same
way `stockBalance` is rolled up there today. Quantities are shown in the product's base unit (rounded down, from the
exact storage figure), with `stockBalanceBreakdown` / `openingStockBreakdown` texts alongside. It does **not** call
`OpeningStockService` to get them — it depends on
`OpeningStockAmountsFetcher`, a separate, dependency-free bean that only
wraps `OpeningStockRepository.findByLocationProductIdIn`.

This split exists to dodge a Spring circular-bean dependency, the exact
trap `organizations/business/opening_balance/README.md` describes for
`AccountDataFetcher`/`OpeningBalanceService`: `OpeningStockService`
depends on `LocationProductService` and `LocationProductDataFetcher` (for
`guardAllActive` and `findSummaryByIds`), and **both of those already
depend on `LocationProductEnricher`** to build their own response DTOs.
If `LocationProductEnricher` depended on `OpeningStockService` too, the
container would have to construct
`OpeningStockService → LocationProductService → LocationProductEnricher
→ OpeningStockService` — a cycle Spring refuses to resolve
(`BeanCurrentlyInCreationException`). `OpeningStockAmountsFetcher` has no
dependency on anything in `location_product`, so `LocationProductEnricher`
can depend on it with no cycle either way.
