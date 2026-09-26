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
future stock-adjustment feature (`MovementType.STOCK_ADJUSTMENT` already
exists as scaffolding, no service implements it yet). That feature —
not this one — is where `StockEntryEntity.priority` gets used
deliberately: an adjustment lot should be ordered relative to the
original opening-stock lot (and any other source) rather than
defaulting to `priority = 0` for everything, the way
`PurchaseDeliveryStockUpdater` and `OpeningStockStockUpdater` both do
today. Making `opening_stock` itself correctable was tried and reverted
— it required drawing down/topping up the original lot via signed
deltas with no real way to express priority for the correction, which is
exactly the adjustment feature's job, not this one's.

## Why a separate table at all

`StockEntryEntity`/`StockMovementEntity` already record the quantity and
cost. `OpeningStockEntity` exists alongside them purely to:

- Anchor the "already declared for this product" guard
  (`OpeningStockRepository.findByLocationProductIdIn`, backed by the
  unique constraint) — the one thing `stock/` has no natural row for.
- Carry its own `HasReferenceEntity` reference number, which becomes both
  `StockEntryEntity.externalReferenceNumber` (idempotency, same as
  `PurchaseDeliveryStockUpdater`'s use of a delivery line's reference) and
  the Kafka event's `sourceReferenceNumber` for ledger posting / DLT
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
5. Guard positive quantity / non-negative unit cost per line, then
   `locationProductService.guardAllActive` — same guard used at
   purchase-creation time.
6. Guard none of the requested products already have an `opening_stock`
   row.
7. Persist the `OpeningStockEntity` rows in one `saveAll` (not a
   per-line `save` in a loop), storing quantity/cost exactly as declared
   — `OpeningStockEntity` has no `unitId` column, unlike `stock/`'s
   entities; the unit only matters for the base-unit conversion below,
   which is `stock/`'s concern, not this domain's.
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
   (`StockEntryEntity.externalReferenceNumber`).
9. Publish one `OpeningStockDeclaredEvent` per line via
   `OpeningStockHandlerForKafka.publish`. Ledger posting still has to be
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
  `productLabel`, `quantity`, `unitCost`, `declaredBy`, `declaredAt`.
- `GET` → `OpeningStockService.getAll` (all declarations for the current
  location, used by the UI to grey out products that already have an
  opening stock).

Both are thin delegators; all validation/locking/posting logic lives in
`OpeningStockService`.

## Rollup onto `LocationProductResponseDto`

`location_product`'s `LocationProductEnricher` surfaces
`openingStockQuantity`/`openingStockUnitCost` (both nullable — absent
means never declared) on every `LocationProductResponseDto`, the same
way `stockBalance` is rolled up there today. It does **not** call
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
