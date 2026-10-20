# ledger

Double-entry postings. Every business event that moves money (sale, payment,
delivery, opening balance, …) is turned into a `LedgerPostingRequest` by a
processor under `processors/` and persisted by `LedgerPostingService.post`
as a `ledger_entry_group` + `ledger_entry` rows (+ `subledger_entry` rows per
contact). Running balances live on the account (see `account/README.md`),
not here.

## Entry type is stated by the processor, never derived

Each `LedgerEntryRequest` carries an explicit `EntryType`
(`LedgerEntryRequest.debit(...)` / `.credit(...)`). `LedgerPostingService`
does not infer it from the account type: the same account is legitimately
debited *and* credited (a receivable rises on a sale, falls on a payment),
so the account alone can't say which one an event is. Keeping direction
independent of account type is also what makes the balanced-entries check
meaningful — a flipped leg fails it instead of producing a balanced, wrong
posting. The processors with a runtime direction (`OpeningBalanceAccountingProcessor`,
`ContactOpeningBalanceAccountingProcessor`, `SaleTaxLedgerEntriesBuilder`) use the `LedgerEntryRequest` constructor.

## What `post` enforces, in order

1. `LedgerEntriesValidator.validate` — total debits equal total credits, and
   no negative amount. Zero is allowed: a 100%-discounted sale posts
   zero-amount legs.
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
  `BalanceSignal` read from the org's side: `MONEY_IN` when they owe us (net up), `MONEY_OUT` when we
  owe them, `ZERO_BALANCE` when equal — the same mapping as `AccountType.balanceSignal()`, where an
  asset is `MONEY_IN` and a liability `MONEY_OUT`. Ledger rows carry the same two fields from their running figures, plus `transactionAmount` and
  `transactionBalanceSignal` from the entry's own payable/receivable delta; the raw payable and
  receivable figures are not exposed.
- `subledger_balance` holds one row per contact with its current `running_payable` /
  `running_receivable`, and `getCurrentBalances` reads it. The row is created with the contact
  (`ContactService.createContact`, and `ContactSeeder` for the system Walk-In contact, both through
  `SubledgerService.openBalance`). Balances are stored at scale 4, and `SubledgerBalanceDto.ZERO`
  matches it. `LedgerPostingService` updates it in the same transaction as each
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
(`ExpenseLiabilityAccount`, in `processors/`).

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
`LedgerPostingRequest` through `api/FundMovementPostingRequests`, which takes the reference, accounts, amount and date as
plain values and picks the source type, so `LedgerSourceType` stays internal to `ledger`. It posts inside the transaction
that saves its row.

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

`loan` posts through `api/LoanPostingRequests`, which takes the reference, asset account, lender contact reference,
amount and date as plain values and picks the source type, so `LedgerSourceType` stays internal to `ledger`. Like
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
Not yet covered: `SaleTaxLedgerEntriesBuilder`.

## Sale cost of goods sold

`SaleConfirmedEventProcessor` adds `COST_OF_GOODS_SOLD` debit / `INVENTORY` credit for `event.cogsTotal` to the sale's
group (dated `dateSold`); `SaleVoidedEventProcessor` posts the reverse (dated `dateVoided`). Both skip only when the
total is exactly zero. A negative total is posted on purpose so `LedgerEntriesValidator` fails loudly.
