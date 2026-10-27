# stock_adjustment

Manual correction of a location product's stock. Every adjustment has a **category** (`StockAdjustmentCategory`,
declared in `stock/api`) that decides its direction, where its unit cost comes from and which ledger accounts it posts
to. `stock_adjustment` rows are **append-only** (`ImmutableEntity`); a wrong adjustment is corrected by a further
adjustment, never edited.

Like `opening_stock`, the row exists next to the real ledger (`stock/StockEntryEntity` / `StockMovementEntity`) to carry
what that ledger has no place for: the typed quantity and unit, the category, the reason, the note and the total cost.
Balances change only through `stock/api/StockAdjustmentStockUpdater`. The category, reason and note are also stored on
every movement the adjustment produces (`StockMovementEntity.category` / `reasonId` / `note`).

## Categories (`StockAdjustmentCategoryRules`)

| Category      | Direction            | Unit cost                         | Debit                                  | Credit                   | Asks for |
|---------------|----------------------|-----------------------------------|----------------------------------------|--------------------------|----------|
| Internal Use  | decrease             | FIFO cost consumed                | `INTERNAL_USE_EXPENSE`                 | `INVENTORY`              | note     |
| Opening Stock | increase             | user-supplied (`unitCost`)        | `INVENTORY`                            | `OPENING_BALANCE_EQUITY` | note     |
| Free Goods    | increase             | user-supplied (`unitCost`)        | `INVENTORY`                            | `FREE_GOODS_RECEIVED`    | note     |
| Other         | user picks           | decrease: FIFO cost consumed; increase: user-supplied (`unitCost`) | decrease `SHRINKAGE_AND_LOSSES`, increase `INVENTORY` | decrease `INVENTORY`, increase `SHRINKAGE_AND_LOSSES` | reason |

The account mapping is hard-coded in `StockAdjustmentCategoryRules.ledgerAccounts`; users cannot configure it. Opening
Stock and Free Goods create their own cost layer, no supplier payable, and are not linked to a purchase order.
`FREE_GOODS_RECEIVED` is an `EXPENSE_CONTRA` under `COST_OF_GOODS`, so profit on free goods is recognised on
receipt, not on sale.

## UI contract (`GET secured/stock-adjustments/categories`)

`StockAdjustmentCategoryDescriptor` per category: `allowedDirections` (one entry means the direction is fixed),
`unitCostDirections` (the directions that need a `unitCost`; for `OTHER` only the increase does, so the UI asks once the direction is chosen), `requiresReason`, `requiresNote`, and `reasons` (only populated when the reason is required; each
carries the directions it allows). The UI renders its form from this and holds no category rules of its own.

## Request (`StockAdjustmentCreateDto`)

`locationProductId`, `category`, `direction` (only for `OTHER`; a fixed-direction category rejects a mismatch),
`quantity` (a `PositiveUserProvidedQuantity`, so positive and at most `QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES` decimals, and
the unit conversion is always exact and nothing is rounded), `unitId`, `stockMovementReasonId`, `note`, `unitCost`.
`unitCost` is per the request's `unitId`, the same unit as `quantity`, as on a purchase line.

`StockAdjustmentValidator` enforces per category: note required except for `OTHER`; reason required for and only
accepted by `OTHER`; `unitCost` (a `PositiveMoney`) required for any increase (Opening Stock, Free Goods, `OTHER` increase) and rejected on a decrease.
The reason is a **stock movement reason** (`StockMovementReasonService.guardReason` with the direction's
`MovementType`), not an `adjustment_reason`: those are discount/surcharge reasons for sales. Reasons are labels with no
ledger account; every `OTHER` reason posts to `SHRINKAGE_AND_LOSSES`.

## Flow (`StockAdjustmentService.adjust`)

1. `StockAdjustmentValidator.guardCategoryRequest` resolves the direction.
2. `fiscalPeriodService.requireOpenForDate(today)`, product advisory lock, `guardAllActive`, and `guardReason` for
   `OTHER` only.
3. Convert once: `getStorageUnitsPerUserProvidedUnit` (rejects a unit outside the product's group), then
   `ExactQuantity.ofStorageFromUserProvided`. That exact value is persisted and passed to the stock updater.
4. The service generates the adjustment's own reference number up front (`ReferenceNumberGenerator`); it becomes the
   entity's `referenceNumber` and the stock layer / movements' `documentReference`. It is generated early because the
   row is immutable and its `totalCost` is only known after the stock change.
5. Increase: `totalCost` is the supplied `unitCost × quantity` for every category. No cost is inferred from existing
   layers: layers differ in source and price, so any one of them would misprice the increase. `increaseStock` creates one new FIFO layer (`StockItemSource.STOCK_ADJUSTMENT`,
   `MovementType.STOCK_ADJUSTMENT_INCREASE`).
6. Decrease: `decreaseStock` rejects when available (on hand minus reserved) is short (`StockAvailabilityValidator`),
   then consumes through `FifoStockConsumer` / `FifoTakePlanner`. The consumed cost (`StockAdjustmentConsumedCost`) is
   the row's `totalCost`.
7. Save the `StockAdjustmentEntity`, then `StockAdjustmentHandlerForKafka.publish`.

New layers use `priority = 0`, like `PurchaseDeliveryStockUpdater` and `OpeningStockStockUpdater`; FIFO then orders by
creation time.

## Ledger posting

`StockAdjustmentRecordedEvent` is published on commit and carries the debit and credit account codes resolved from the
category and direction. `StockAdjustmentAccountingProcessor` posts them under `LedgerSourceType.STOCK_ADJUSTMENT`,
idempotent on the adjustment's reference number. A zero-cost adjustment publishes nothing. `totalCost` travels as
`Money`, so a total above the ceiling fails at publish, inside the adjustment's transaction.

## REST surface

`StockAdjustmentEndpoint` (`secured/stock-adjustments`): `POST` → `adjust`, `GET` → `getAll`, `GET categories` →
`getCategoryDescriptors`. All are thin delegators.
