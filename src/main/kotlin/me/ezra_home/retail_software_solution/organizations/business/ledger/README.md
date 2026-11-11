# ledger

Double-entry postings. Every business event that moves money (sale, payment,
delivery, opening balance, …) is turned into a `LedgerPostingRequest` by a
processor under `processors/` and persisted by `LedgerPostingService.post`
as a `ledger_entry_group` + `ledger_entry` rows (+ `subledger_entry` rows per
contact). `processors/` holds only event processors; the classes they share
(`LedgerPostingGate`, `SaleTaxLedgerEntriesBuilder`, `ExpensePostingRequests`, `ExpenseLiabilityAccount`) live in
`helpers/`. `PostedStockAdjustmentAccountsReader` is in `api/`, because the delivery revision preview in the locations
layer reads it too. Running balances live on the account (see `account/README.md`), not here.

## Entry type is stated by the processor, never derived

Each `LedgerEntryRequest` carries an explicit `EntryType`
(`LedgerEntryRequest.debit(...)` / `.credit(...)`). `LedgerPostingService`
does not infer it from the account type: the same account is legitimately
debited *and* credited (a receivable rises on a sale, falls on a payment),
so the account alone can't say which one an event is. Keeping direction
independent of account type is also what makes the balanced-entries check
meaningful — a flipped leg fails it instead of producing a balanced, wrong
posting. The processors with a runtime direction (`OpeningBalanceAccountingProcessor`,
`ContactOpeningBalanceAccountingProcessor`, `SaleTaxLedgerEntriesBuilder`) use the `LedgerEntryRequest` constructor,
or `LedgerEntryRequest.unlessZero` where the amount can be zero (`SaleTaxLedgerEntriesBuilder`).

## Zero-amount legs are omitted

A leg whose amount can legitimately be zero (a 100%-discounted sale's receivable, a zero-cost COGS pair, a zero
tax) is built with `debitUnlessZero` / `creditUnlessZero` / `unlessZero`, which return no leg
for an exact zero; callers concatenate the lists. Omitting a leg cannot unbalance a posting, because a zero leg adds
nothing to either side. A posting that ends up with no legs at all still saves its `ledger_entry_group`, which is what
`shouldProcess` checks to stay idempotent. Amounts that are positive by their source (loans, fund movements,
expenses, payments, opening-balance changes) use `PositiveMoney.of` directly, so a zero there fails instead of being skipped. A negative
amount is never skipped: it reaches `PositiveMoney.of` and fails.

## What `post` enforces, in order

1. `LedgerEntriesValidator.validate` — total debits equal total credits. Each
   leg's amount is a `PositiveMoney` (greater than zero, at most the money scale
   in decimals), so every sum over the ledger (daily balances and performance,
   their totals) is exact at that scale. A zero or negative amount cannot be
   built.
2. An open fiscal period exists for `postingDate`.
3. `AccountService.assertPostable` — every account is known, has no
   children, and is not an inactive account being *increased* (drawing an
   inactive account down is allowed, which is what lets voids and
   reversals still unwind). Direction is judged per entry against the
   account's own `normalBalance`.
4. Entries saved, with `AccountsAndLedgerLock` held (see below), then `AccountService.patchBalances` updates running
   balances in each account's own normal direction.

## Locking: `AccountsAndLedgerLock`

`post` takes `AccountsAndLedgerLock` before `assertPostable`, and holds it for the
transaction. One entry point, separate namespaces and ids:

- `LockNamespaces.ACCOUNT` per account code (the same lock
  `AccountStructureLock` gives `createChild`): `assertPostable` reads "has no
  children" and the posting then writes, so a child added in between would
  leave postings on a non-leaf. Sorted acquisition also keeps two postings
  from taking account row locks in opposite orders in `patchBalances`.
- `LockNamespaces.CONTACT` per contact reference number: `subledger_entry` is
  append-only and each row is the latest row plus a delta. A row lock
  (`FOR UPDATE`) on the latest row can't serialize that — a concurrent posting
  locks the same row, waits, and computes from it without seeing the row the
  first posting inserted, losing that delta; a contact's first row has nothing
  to lock at all.

Accounts are always acquired before contacts. `acquire` takes a
`LedgerPostingRequest`. `account` never depends on `ledger`: year-end close
takes the closing accounts and Retained Earnings through
`AccountStructureLock` directly, and `ledger` hands `account` its posting lines
as `AccountPosting`s.

## Subledger columns

`receivable` is the column that moves a contact's net (`receivable - payable`) **up**, i.e. toward
them owing us; `payable` is the column that moves it **down**. Both only ever grow — a reversal
posts to the opposite column rather than a negative amount. A debit on `TRADE_RECEIVABLES`,
`TRADE_PAYABLES` or an expense liability moves net up; a credit moves it down.

`SubledgerEntryRequest.payableAmount` and `receivableAmount` are `Money` (zero allowed, never negative): one column
carries the amount and the other is `Money.ZERO`, so a `PositiveMoney` would reject the unused column. A negative
amount fails in `Money.of` when the request is built. The entities, the running figures and the `SUM` projections stay
`BigDecimal` because a running total can exceed the single-transaction maximum `Money` enforces. `SubledgerService` reads
entities directly (there is no entry DTO) and `SubledgerBalanceDto` holds the exact `BigDecimal` figures. A response shows
them as `MoneyDisplay`, so an oversized running balance rounds instead of throwing.

| Posting | Control-account leg | Column |
|---|---|---|
| sale, sale payment void | debit receivables | receivable |
| sale void, sale payment | credit receivables | payable |
| supplier payment, expense void, expense payment | debit payables / liability | receivable |
| delivery, supplier payment void, expense, expense payment void | credit payables / liability | payable |
| contact opening balance: receivable raised, payable reduced | debit receivables / payables | receivable |
| contact opening balance: payable raised, receivable reduced | credit payables / receivables | payable |
| loan, loan repayment void | credit `LOANS_PAYABLE` | payable |
| loan void, loan repayment | debit `LOANS_PAYABLE` | receivable |

## Reading the subledger: `SubledgerService`

`api/SubledgerService` is the only read path other domains use.

- `getCurrentBalances` returns each contact's current `running_payable` /
  `running_receivable`; `ContactService` uses it for the glance balances on
  `ContactResponseDto` (a contact with no rows reads as `SubledgerBalanceDto.ZERO`).
  `SubledgerBalanceDto` also derives `netBalance` (absolute `receivable - payable`) and a
  `BalanceEffect` read from the org's side: `ADDS` when they owe us (net up), `SUBTRACTS` when we
  owe them, `ZERO` when equal — the same mapping as `AccountType.balanceEffect()`, where an
  asset is `ADDS` and a liability `SUBTRACTS`. Ledger rows carry the same two fields from their running figures, plus `transactionAmount` and
  `transactionBalanceEffect` from the entry's own payable/receivable delta; the raw payable and
  receivable figures are not exposed.
  A contact has no normal side, so a subledger effect is never `IRREGULAR`; the net of a contact can legitimately sit on
  either side (advances, credit notes, deposits).
- `subledger_balance` holds one row per contact with its current `running_payable` /
  `running_receivable`, and `getCurrentBalances` reads it. The row is created with the contact
  (`ContactService.createContact`, and `ContactSeeder` for the system Walk-In contact, both through
  `SubledgerService.openBalance`; the bulk contact import opens every new contact's row at once through
  `SubledgerService.openBalances`). Balances are stored in `numeric(19,4)` columns, and `openBalance` seeds
  them from `SubledgerBalanceDto.ZERO`, which matches that scale. `LedgerPostingService` updates it in the same transaction as each
  `subledger_entry` insert, under the `SUBLEDGER_CONTACT` advisory lock, and fails the posting if
  the contact has no row. Besides posting, only `SubledgerService.reconcileBalances` writes it:
  `SubledgerBalanceReconciliationJob` runs it for every provisioned organization nightly at 02:30,
  which also catches up an organization that predates the table and resets any drifted row. It
  locks the contacts, then sets each row to the sum of that contact's entry amounts (creating
  missing rows) and logs a warning for every row it had to change. `ContactSeeder` also
  runs it after seeding, so seeding defaults for an organization creates any missing rows and
  corrects drift immediately. That hook is a stopgap until reconciliation has its own trigger.
- `subledger_entry` stays the append-only history; each row carries the running figures as of its
  posting. The balance row is a derived total and is not audited. Entries are ordered by
  `created_on` for display only; no total depends on that order, so clock skew between nodes
  cannot corrupt a balance.
- `getEntries` pages one contact's rows newest first, keyset on `(created_on, id)` via
  `KeysetSearchCursor`, over-fetching one row to set `hasMore`. Because the query is filtered by
  the contact's reference number, a cursor only means something against the contact it came
  from — each contact pages its own history. Page size is 1..200. Each row is enriched with the
  source type and source reference of its `ledger_entry_group`, fetched in one query per page.
- Exposed as `POST secured/contacts/{id}/ledger` with the standard `PageRequest` body (`parameters` is an empty object); contacts are
  never deleted, only status-changed, so the history always has an owner.

- Contact opening balances post through `ContactOpeningBalanceAccountingProcessor`; a downward
  revision goes in the opposite column like any other reversal. See `contact_opening_balance/README.md`.

## Expense processors

`ExpensePostingRequests` builds all four expense postings; the four processors
(`Expense{Recorded,Voided,PaymentRecorded,PaymentVoided}AccountingProcessor`) extend
`ExpenseAccountingProcessor`, which gates `shouldProcess` through `LedgerPostingGate` and
resolves the payee's contact reference; each subclass supplies only its source type, reference,
payee and request. They serve both location and org expenses.

| Event | Entries | Subledger (payee) |
|---|---|---|
| recorded | debit expense account, credit liability | payable raised |
| voided | credit expense account, debit liability | payable reduced |
| payment recorded | debit liability, credit payment-method account | payable reduced |
| payment voided | credit liability, debit payment-method account | payable restored |

The liability is derived at posting time from the expense account snapshotted on the expense row (`expense_account_code`, copied from the expense type when recorded and never re-read from it): `WAGES_EXPENSE`
credits `WAGES_PAYABLE`, every other account credits `TRADE_PAYABLES`
(`ExpenseLiabilityAccount`, in `helpers/`).

Voiding an expense is the only entry that credits the expense account; a refund-style
reversal of a payment never touches it.

Idempotency is keyed on `(reference, source type, location)` in `ledger_entry_group`. A
void and its original share a reference and differ only by source type. A void's
`shouldProcess` only checks that the void itself is not yet posted; preparing the void
requires the original posting and throws if it is missing, so the event fails, is logged and
can be retried once the original posts. A skipped void would never be revisited. The same rule
holds for `SupplierPaymentVoidAccountingProcessor`, `SaleVoidedEventProcessor` and
`SalePaymentVoidedEventProcessor`. `LedgerPostingGate` picks the
location-keyed or the `...SourceLocationIdIsNull` check from the event's `sourceContext`,
and a location-level event processed outside that location's session is rejected rather than
keyed elsewhere. An org-level expense is stored with a null `source_location_id`, which
is what separates it from a location expense. Both share the `EXPENSE*` source types, and
the two partial indexes (`uq_ledger_entry_grp_ref_type_loc` / `_org`) keep their
references from colliding. `idempotencyConstraintName` follows the same session check.

## Fund movement (synchronous caller)

`fund_movement` is, with `loan` (below), a caller of `LedgerPostingService.post` outside the event processors. It builds the
`LedgerPostingRequest` through `FundMovementPostingRequests` (in `fund_movement`), which takes the reference, accounts,
amount and date as plain values and picks the source type from the public `api/LedgerSourceType`. It posts inside the
transaction that saves its row.

| Event | Entries | Posting date |
|---|---|---|
| fund movement | debit the row's `debit_account_code`, credit its `credit_account_code` | `movement_date` |
| fund movement void | the same two accounts with the legs swapped | the void's `void_date` (today) |

No subledger entries. The org-level request carries no location, so `source_location_id` is null and the group falls
under `uq_ledger_entry_grp_ref_type_org`, which keys `(reference, source type)` for any source type. A void shares its
movement's reference and differs only by source type. A posting failure (closed fiscal period, an inactive account being
increased) propagates and rolls the movement row back; a void is the exception for inactive accounts: its request sets
`reversal = true`, so `post` calls `AccountService.assertPostableReversal`, which lets an inactive account be built up but
still rejects an unknown account or one with children; there is no event, DLT or reissue. See `fund_movement/README.md`.

## Loans (synchronous caller)

`loan` posts through `LoanPostingRequests` (in `loan`), which takes the reference, asset account, lender contact reference,
amount and date as plain values and picks the source type from `api/LedgerSourceType`. Like
`fund_movement`, it calls `LedgerPostingService.post` inside the transaction that saves its row; a posting failure rolls
the row back. See `loan/README.md`.

| Event | Source type | Entries | Posting date | Subledger (lender) |
|---|---|---|---|---|
| loan | `LOAN` | debit asset account, credit `LOANS_PAYABLE` | `loan_date` | payable raised |
| loan void | `LOAN_VOID` | debit `LOANS_PAYABLE`, credit asset account | the void's `void_date` | receivable raised |
| loan repayment | `LOAN_REPAYMENT` | debit `LOANS_PAYABLE`, credit asset account | `repayment_date` | receivable raised |
| loan repayment void | `LOAN_REPAYMENT_VOID` | debit asset account, credit `LOANS_PAYABLE` | the void's `void_date` | payable raised |

A void shares its original's reference and posts as a reversal (`reversal = true`), so a loan on a since-deactivated
asset account can still be voided. Org-level requests carry no location, so `source_location_id` is null and the groups
fall under `uq_ledger_entry_grp_ref_type_org`, which keys `(reference, source type)` for any source type.

## Reading movements: `LedgerMovementReader`

`api/LedgerMovementReader` is the read path for `daily_business_summary`: summed entry amounts grouped by local date
(`ledger_entry_group.posted_on` bucketed with `width_bucket` over day starts computed in Kotlin in the organization
zone), account, entry type and, for `getMovements`, source. `getMovements` covers `[from, before)` for every account, a
`LedgerSourceType` whitelist and an optional location; `getAccountMovementsFrom` is organization-wide, all source
types, from the start of `from` onward, reporting anything dated at or after `before` under `before`.
`idx_ledger_entry_grp_posted_on` serves the range scan.

`ledger_entry_group.posted_on` holds the start of the posting date in the organization zone, not the write time, which
is `created_on`. A group therefore reports on the day it was dated, including backdated manual journals and any other
event that carries its own date.

## Contra accounts

Contras are posted like any other account, in their own normal direction
(`SALES_DISCOUNTS` is debited by a sale). `account`'s rollup subtracts them
from their parent for display.

## Testing

`LedgerPostingBalanceTest` and `SupplierAndOpeningPostingBalanceTest` run
each processor's real output through the real `AccountService.patchBalances`
(via `RecordingAccountService`) and assert the resulting balances. Add a case
there when adding or changing a processor — the balanced-entries check
cannot catch a processor that flips both legs.
Not yet covered: `SaleTaxLedgerEntriesBuilder`. `RevisionInventoryConsistencyTest` checks that the three stock revision
postings move inventory by the stock value change.

The synchronous callers' builders are tested in their own domains (`FundMovementPostingRequestsBalanceTest`,
`LoanPostingRequestsBalanceTest`), which run each request's entries through `AccountService` and check the subledger
amounts on the request. The posting rules they rely on (closed fiscal period, reversal postability, null source
location) are covered by `LedgerPostingServiceOrgLevelTest`.

## Event amounts

The sale, delivery, adjustment and opening-stock processors read typed `Money` amounts off the event and never form a
sum themselves, so a ceiling breach cannot surface in the consumer (see `util/business/README.md` rule 9).

## Sale cost of goods sold

`SaleConfirmedEventProcessor` adds `COST_OF_GOODS_SOLD` debit / `INVENTORY` credit for `event.cogsTotal` to the sale's
group (dated `dateSold`); `SaleVoidedEventProcessor` posts the reverse (dated `dateVoided`). Both skip the pair only
when the total is exactly zero. A negative total is not skipped: `PositiveMoney.of` rejects it, so the posting fails
loudly.

## Stock transfer postings

A transfer posts balance-sheet entries only (`postsToProfitAndLoss = false`), so goods in transit are an asset, not a cost.
Each group is keyed on the **dispatch's** reference and differs by source type and location; the amount is the dispatch's
total cost, carried on each event as `totalCost`.

| Source type               | Processor                                  | Location    | Posts                                  |
|---------------------------|--------------------------------------------|-------------|----------------------------------------|
| `STOCK_TRANSFER_DISPATCH` | `StockTransferDispatchAccountingProcessor` | source      | Dr Inventory in transit, Cr Inventory  |
| `STOCK_TRANSFER_RECEIPT`  | `StockTransferReceiptAccountingProcessor`  | destination | Dr Inventory, Cr Inventory in transit  |
| `STOCK_TRANSFER_CANCEL`   | `StockTransferCancelAccountingProcessor`   | source      | Dr Inventory, Cr Inventory in transit  |

The in-transit balance is therefore positive at the source location and negative at the destination while a transfer is
open, and nets to zero organization-wide. A receipt or cancel clears an in-transit balance only after the dispatch's own
posting exists at the source (`StockTransferLedgerPostings.requireDispatchPosted` for the receipt, `LedgerPostingGate.requirePosted`
for the cancel). Until it does, they fail like the voids above: the dispatch and receipt events sit on different partitions, so
a receipt can be consumed before its dispatch posts, and the failed event is retried by the sweeper until the dispatch lands. A
zero-cost dispatch posts nothing, so its receipt and cancel are not processed either.

## Stock revision postings

A stock revision (a delivery line revised after other documents drew from its stock) posts up to three groups. Each is
keyed on the revision's reference, so a revision posts one group per source type rather than one per affected document, and
each is dated today, in the current period. The postings are skipped when their amount is unchanged.

| Source type                     | Processor                                      | Posts                                                                                    |
|---------------------------------|------------------------------------------------|------------------------------------------------------------------------------------------|
| `PURCHASE_DELIVERY_REVISION`    | `PurchaseDeliveryRevisionAccountingProcessor`  | Dr Inventory, Cr Trade payables for a dearer line; reversed for a cheaper one          |
| `SALE_COGS_RESTATEMENT`         | `SaleCogsRestatementAccountingProcessor`       | Dr Cost of goods sold, Cr Inventory when the sales' cost rises; reversed when it falls |
| `STOCK_ADJUSTMENT_RESTATEMENT`  | `StockAdjustmentRestatementAccountingProcessor`| Each restated adjustment's own legs for a rise, reversed for a fall, netted by account |

The entries are built by `RevisionLedgerEntries` (`locations/business/stock_revision/api`), which the processors and
the delivery revision preview both call, so the preview shows what will post. The supplier subledger is additive and never negative, so a cheaper line posts to the receivable side, not a negative
payable. The adjustment restatement takes its accounts from the original `STOCK_ADJUSTMENT` posting
(`api/PostedStockAdjustmentAccountsReader`), so a later change to the category rules cannot split an adjustment across
two sets of accounts; the accounts the current rules give are used only for an adjustment whose original cost was zero, which
posted no entries. An adjustment with no original posting fails the event, which the event log sweeper retries.

The sale and adjustment restatements count in the daily profit and loss; the purchase revision does not. Every
`LedgerSourceType` states this in its `postsToProfitAndLoss` flag, and `LedgerSourceType.profitAndLossSources` is the set
`DailyPerformanceService` reads, so a new source type decides it when it is declared. All three processors declare
`idempotencyConstraintName` from `LedgerPostingGate`, so only a lost race on the group's own unique index is benign.

After a revision the ledger's inventory moves by the change in the stock on hand's value: the line total change minus the
change in what the consumers drew from it. This holds only while every consumer type a revision can replay has a
restatement (`RestatableConsumers` in `stock_revision`); `RevisionInventoryConsistencyTest` runs the three processors and
checks it.
