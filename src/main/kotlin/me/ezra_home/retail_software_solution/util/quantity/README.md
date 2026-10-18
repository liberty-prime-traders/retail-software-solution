# util/quantity — exact quantities

Stock is counted in the **storage unit**: the root of the product's unit group (see
`organizations/business/unitvalue/README.md`). Every conversion into stock is a multiplication by a whole number, so
nothing rounds on the way in and there is no rounding policy to manage.

## Rules

1. **Storage quantity = user-provided quantity x the line's captured `storage_units_per_unit`.** The ratio is the line
   unit's size in the root, captured when the line is created or its unit changes, and used from then on. It is one
   `BIGINT` column, at least 1.
2. **`ExactQuantity` is the only type for quantity arithmetic and comparison across units.** Build it with
   `ofUserProvided(userProvidedQuantity, storageUnitsPerUnit)` or `ofStorage(storageQuantity)`, never from a number
   that was already converted or rounded. Sufficiency checks compare exactly and strictly (available >= requested),
   with no tolerance.
3. **No quantity is ever divided on its way to stock.** The only divisions are display figures
   (`ExactQuantity.inUnitOfSize`), computed once from the exact value, at scale 4, with the rounding mode named by the
   caller. Never multiply a quantity by an already-rounded factor.
4. **No code outside `util/quantity` and `util/business` multiplies or divides a quantity by a conversion ratio.**
   `ArchitectureTest` checks that no other class reads `ConversionRatio`'s `numerator`, `denominator`, `factor()` or
   `displayFactor()`. Price conversion (`convertPriceRoundingHalfUp`) stays allowed.
5. **Precision.** User-provided quantity columns are `numeric(19,4)`, storage quantity columns `numeric(28,4)`, and
   rates and prices `numeric(28,10)`. A user types at most `QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES` (2)
   decimals, and a price override at most `MAX_PRICE_OVERRIDE_DECIMAL_PLACES` (2). The columns hold 4, so raising a
   limit never causes stock rounding. `Decimals.requireMaxDecimalPlaces` enforces a limit in the `init` block of a
   request class, and `GlobalExceptionHandler.handleUnreadableBody` returns the resulting `RtsGenericException` as the
   business error instead of a 500. Quantities the system derives (storage quantities, cost-group shares, remainders)
   are exempt and keep 4 decimals. `ExactQuantity.toStorageQuantity()` throws instead of rounding if a digit beyond
   scale 4 would be lost.
6. **Display is a breakdown.** `QuantityBreakdown` renders a storage quantity from the product's base unit down to the
   root, largest first, whole units at each level, the remainder carried down. The root level may carry a fractional
   remainder, shown with up to 4 decimals and trailing zeros stripped ("2 Bales 0.5 Pieces"). Zero levels are
   skipped, and zero stock shows "0" in the product's base unit.
   - Units larger than the base unit are never used. A non-root unit of the base unit's size, other than the base unit
     itself, is skipped. A non-root unit the size of the root is skipped, because the root carries the remainder.
   - Equal-size units are taken in `createdOn`, then `id` order, so the first takes the quantity and the rest render
     nothing. Siblings (neither a whole number of the other) are taken greedily, larger first.
   - Build one plan per base unit from the organization's units fetched once per request, never per row.
   - Names are pluralized by `DisplayFormatters.pluralize`.
   - A balance shown live is rendered with the current unit sizes. A balance shown in stock history is the text stored
     with its movement and is never re-rendered.

## Worked example (Bale = 18 Pieces, storage unit Piece)

| Stored storage quantity | Shown                |
|-------------------------|----------------------|
| 26                      | 1 Bale 8 Pieces      |
| 17,995                  | 999 Bales 13 Pieces  |
| 36.5                    | 2 Bales 0.5 Pieces   |
| 0                       | 0 Bales              |

Selling 1,000 Bales one Piece at a time, 18,000 times, leaves exactly 0.
