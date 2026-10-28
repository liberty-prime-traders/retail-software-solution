# daily_business_summary

Two read-only, request-time views over the ledger for a local date range. Nothing is stored; every figure is
recomputed on each call. Exposed by `DailyBusinessSummaryEndpoint` at `POST secured/daily-business-summary/performance`
and `.../balances` (`CHART_OF_ACCOUNTS`), reachable from organization and location sessions alike. The two calls run
on the request thread; the client issues them concurrently.

Depends on `ledger/api` (`LedgerMovementReader`) and `account/api` (`AccountService.getAccountTree`,
`getFreshBalancesByCodes`) only.

## Shared rules

- `from` is inclusive, `before` exclusive, both local dates in the organization zone. `SummaryRange` guards
  `from < before`, `before` not after tomorrow, and at most 92 days between them.
- A ledger group belongs to the local date of `ledger_entry_group.posted_on`. Day-start boundaries are computed in
  Kotlin (`DateTimes.Offset.atStartOfDayInOrganizationZone`) and bucketed with `width_bucket` inside `ledger`; the
  organization zone can be a fixed offset, which Postgres `AT TIME ZONE <string>` reads with the opposite sign.
- A void posts on the day it was processed, so it lowers that day's figures, never the original's.
- Every figure is the account's movement in its own normal direction (`AccountTreeNode.normalBalance`), so a debit to
  `GROSS_SALES` reduces it. Direction is never inferred from the source type.
- Every response has one entry per local date, ascending, zero-filled. `PeriodStatus.IN_PROGRESS` marks the local day
  containing `asOf` and a range whose last day is that day.
- Amounts are scale 2 (cents) and signed. Account names are `AccountTreeNode.name` (the `AccountService` label).

## Performance (`DailyPerformanceService`)

Scope comes from the session: an organization session reads every location plus organization-level entries; a
location session keeps only groups whose `source_location_id` equals it, which drops organization-level entries.

Source whitelist, requested explicitly from the reader:

- profit and loss: `SALE`, `SALE_VOID`, `EXPENSE`, `EXPENSE_VOID`, `STOCK_ADJUSTMENT`
- money collected and receivables: `SALE`, `SALE_VOID`, `SALE_PAYMENT`, `SALE_PAYMENT_VOID`

Opening balances, opening stock, deliveries, supplier payments, expense payments and fund movements never contribute.

| Field | Definition |
|---|---|
| `netSales` | `GROSS_SALES` − `SALES_DISCOUNTS` − (`SALES_RETURNS` + `SALES_ALLOWANCES`) |
| `costOfGoodsSold` | `COST_OF_GOODS_SOLD` − `FREE_GOODS_RECEIVED` (contra) |
| `expenseLines` | non-zero `EXPENSE` (non-contra) accounts other than `COST_OF_GOODS_SOLD`, by code; includes `TAX_EXPENSE`; a line can be negative |
| `netProfit` | `grossProfit + otherIncome − expensesTotal` |
| `collectedByAccount` | net of `SALE_PAYMENT` / `SALE_PAYMENT_VOID` per account other than `TRADE_RECEIVABLES`, non-zero only |
| `billedToCustomers` | net `TRADE_RECEIVABLES` movement from `SALE` / `SALE_VOID` |

Margins are percent at scale 1 and null when `netSales` is zero. `totals` sums the days' raw components and
recomputes margins from the sums, never averaging.

## Balances (`DailyBalancesService`)

Organization-wide and all source types: accounts, opening balances and fund movements belong to the organization.
A location session reads the same organization-wide figures: `current_balance` is one running total per account for
the whole organization, so no location-scoped closing balance can be derived from it. Per-location balances would need a
location dimension on the running balance, and the route is gated only by the `CHART_OF_ACCOUNTS` feature, not by a
permission of its own. Both are known limits, to revisit if location-level reporting or a finer permission is needed.

`closingBalance(D) = current running balance − net movement posted after the end of D`, in the account's normal
direction. The movements query spans the start of `from` onward, so its cost grows with how far back `from` is, not
with the ledger. Movements dated at or after `before` are subtracted from every closing. `change(D)` is the day's net
movement and `rangeBalances.change` the movement over the whole range. For the day containing `asOf` the closing is the
balance as of `asOf`.

`getBalances` runs at `REPEATABLE_READ` in its own transaction (`REQUIRES_NEW`, so a caller's transaction cannot
override the isolation) so the balance read and the movement read share one snapshot —
`LedgerPostingService` updates the balance and inserts the entries in one transaction, and two snapshots could see half
of a posting. The balances are read from the rows, not the cached tree.

`BalanceSectionBuilder` turns the per-account closing and change maps into the sections. Lines are leaf accounts only. Assets: `CASH`, each leaf under `DIGITAL_PAYMENTS` (itself when childless),
`TRADE_RECEIVABLES`, `INVENTORY`, `Other assets`. Liabilities: `TRADE_PAYABLES`, `WAGES_PAYABLE`, `LOANS_PAYABLE`, each leaf under
`TAX_PAYABLE` (itself when childless), `Other liabilities`. The `Other` lines (null `accountCode`) hold every remaining
asset-/liability-type leaf net of its contras, so each section total is the whole chart's. Fixed accounts are always
present; extension-point leaves and `Other` lines appear only when the closing or change is non-zero for that period.
`netWorth = assets − liabilities` at recorded cost (no revaluation), derived from the sections.

Every line carries `closingBalanceEffect` and `changeEffect`, and the sections carry `changeTotalEffect`, `netWorthEffect` and `netWorthChangeEffect`
(`BalanceEffect`), so the UI never infers colour from the section. A closing balance is `IRREGULAR` when it is negative (the
wrong side for its category), else `ADDS` for assets and `SUBTRACTS` for liabilities. A change is judged by what it does to
net worth: an asset rising or a liability falling `ADDS`, an asset falling or a liability rising `SUBTRACTS`, zero is `ZERO`.
Net worth itself is `ADDS` or `SUBTRACTS` by sign, never `IRREGULAR`.

Year-end close touches only closing-type accounts and Retained Earnings, so it does not affect these balances.

## Testing

`DailyPerformanceServiceTest` and `DailyBalancesServiceTest` fabricate movements through a mocked
`LedgerMovementReader`. The `width_bucket` SQL runs only against Postgres: `LedgerMovementQueriesPostgresTest` (cucumberTest source set, Testcontainers) executes the `@Query` text of `LedgerEntryRepository` with the day-start literal `LedgerMovementReader` builds, at a fixed-offset organization zone.
