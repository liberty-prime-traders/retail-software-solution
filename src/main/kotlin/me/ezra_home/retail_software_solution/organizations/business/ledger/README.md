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
`SaleTaxLedgerEntriesBuilder`) use the `LedgerEntryRequest` constructor.

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

## Expense processors

`ExpensePostingRequests` builds all four expense postings; the four processors
(`Expense{Recorded,Voided,PaymentRecorded,PaymentVoided}AccountingProcessor`) only gate
`shouldProcess` through `ExpenseLedgerGate` and resolve the payee's contact reference.
They serve both location and org expenses.

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
void and its original share a reference and differ only by source type, so a void's
`shouldProcess` also requires the original posting to exist. `ExpenseLedgerGate` picks the
location-keyed or the `...SourceLocationIdIsNull` check from the event's `sourceContext`,
and a location-level event processed outside that location's session is rejected rather than
keyed elsewhere. An org-level expense is stored with a null `source_location_id`, which
is what separates it from a location expense. Both share the `EXPENSE*` source types, and
the two partial indexes (`uq_ledger_entry_grp_ref_type_loc` / `_org`) keep their
references from colliding. `idempotencyConstraintName` follows the same session check.

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
