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
