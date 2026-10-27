# util/business — typed decimals and rounding

## Which type do I use

| The value is                                          | Use                                                     | Scale                                        |
|-------------------------------------------------------|---------------------------------------------------------|----------------------------------------------|
| An amount a user types (zero allowed)                 | `Money`                                                 | `Decimals.MoneyScale` (2)                    |
| An amount a user types, zero not allowed              | `PositiveMoney`                                         | `Decimals.MoneyScale` (2)                    |
| A percentage a user types (0 to 100)                  | `Percentage`                                            | 2 decimals, whatever the money scale         |
| A quantity a user types                               | `UserProvidedQuantity` / `PositiveUserProvidedQuantity` | up to 2 decimals (`util/quantity/README.md`) |
| A quantity in stock or any cross-unit arithmetic      | `ExactQuantity`                                         | `Decimals.Scale4` (4)                        |
| A derived or stored amount (totals, balances, signed) | `BigDecimal`, rounded once with `Decimals.MoneyScale`   | 2 in code, `numeric(19,4)` in the column     |
| A unit price, unit cost or conversion factor          | `BigDecimal` through `Decimals.Scale10`                 | 10                                           |

## Rules

1. **Typed decimals are validated input.** `Money`, `PositiveMoney`, `Percentage`, `UserProvidedQuantity` and
   `PositiveUserProvidedQuantity` share the `TypedDecimal` base. Each rejects a bad value in its own `of` instead of
   rounding, binds from and serializes to a bare JSON number, and is equal only to the same type with the same number.
   They are `Serializable` because cached request DTOs hold them. Entities, derived amounts and signed balances stay
   `BigDecimal`, because a typed decimal rejects a negative and snaps to its own scale.
2. **`Money` rejects** a negative, more decimals than `Decimals.MoneyScale`, and a magnitude above
   `Decimals.MAXIMUM_TRANSACTION_AMOUNT`. `Money.ZERO` is the constant for a column that is always zero, such as the unused
   side of a subledger entry. It allows zero (a purchase line's unit cost, which a draft may leave at zero, a
   location product's `defaultSalePrice`, a contact's `creditLimit`, a tax rate's `rateFlatAmount`, an opening balance,
   and the nullable sale-session price override, where null means unchanged). It normalizes to the money scale (`1.5`
   becomes `1.50`), so it never rounds. `PositiveMoney` also rejects zero (loan, fund movement, payment and expense
   amounts, and the unit cost of a delivery line, an opening stock line and a stock increase).
3. **`Percentage`** is 0 to 100 with at most 2 decimals. An adjustment is a `Percentage` or a `PositiveMoney` on
   `SaleSessionAdjustmentAddDto`; the internal `SaleAdjustmentCreateDto.value` is a plain `BigDecimal` built from session
   state.
4. **Unwrap at the boundary.** `MoneyQualifier` converts the money types to `BigDecimal` in every MapStruct mapper;
   services call `toBigDecimal()`. `toMoney` and `toPositiveMoney` wrap a stored `BigDecimal` back into the typed value
   for the ledger and subledger DTOs. `Decimals.requireMaxDecimalPlaces` stays for limits with no dedicated type.
5. **Rounding goes through `Decimals`.** `Decimals.MoneyScale` (the `MONEY_SCALE` constant), `Scale4`,
   `Scale10` and `PercentDisplayScale` own `round`, `multiply` and `divide`. `ArchitectureTest` rejects a direct
   `BigDecimal.setScale` or `divide` with a `RoundingMode` outside `util.business` and `util.quantity`.
6. **Ceilings are independent.** `MAXIMUM_TRANSACTION_AMOUNT` (money) and `QuantityLimits.MAX_USER_QUANTITY` (quantity)
   happen to be equal; each states its own headroom against its column.
7. **Money columns are `numeric(19,4)`, code rounds to 2.** The two spare decimal places are headroom, never stored
   precision. The 12 integer-digit ceiling (`MAXIMUM_TRANSACTION_AMOUNT`) leaves room in the 15 the column holds for
   balances to sum.
8. **Events carry typed money.** A transaction event holds `Money` for every amount a ledger processor posts, and
   forms any sum a processor needs (`SaleConfirmedEvent.amountOwed`, `grossRevenue`) with `Money.plus` in the event
   itself. The ceiling is therefore enforced where the event is built, inside the transaction that writes the
   document, so an oversized total rolls that transaction back; a processor never meets it after commit.
