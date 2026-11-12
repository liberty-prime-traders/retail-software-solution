# opening_balance

Lets org admins declare a starting balance for any postable (leaf, active)
account, except `TRADE_RECEIVABLES`, `TRADE_PAYABLES` and `LOANS_PAYABLE`: those are the
subledger control accounts, rejected by
`OpeningBalanceAccountValidator.assertManualOpeningBalanceAccepted`. The totals of the first two must equal the
sum of the contacts' balances and so can only come from contact opening balances
(`contact_opening_balance/README.md`); `LOANS_PAYABLE` must equal the sum of outstanding loans and so comes only from
loans (`loan/README.md`), and its rejection message says so. `opening_balance` rows are **append-only** — a correction inserts a
new row rather than mutating an old one, so every past declaration stays
exactly as it was posted.

## Why append-only

- The "current" balance for an account is just its latest row
  (`OpeningBalanceRepository.findLatestForAccountCode`, `ORDER BY created_on
  DESC LIMIT 1`). Reading it and inserting the new row is not a
  read-modify-write on that same row, so a row lock (`SELECT ... FOR UPDATE`)
  would not stop a second transaction from reading the same "latest" row and
  inserting its own sibling from it — nothing about the winner's commit
  changes the row the loser locked. `OpeningBalanceService.upsert` instead
  takes `OrgEntityAdvisoryLock.acquire(LockNamespaces.ACCOUNT, accountCode)`
  for the whole transaction, which does block the second reader outright —
  mirroring `locations/business/lock`'s `EntityAdvisoryLock`, routed through
  the organization schema's `EntityManagerFactory` instead of the location
  one, since `pg_advisory_xact_lock` is connection-scoped and this domain
  lives on the organization datasource.
  `LockNamespaces.ACCOUNT` is namespaced by the account resource, not by
  this feature, because `AccountService.createChild` takes the same lock
  (on `parentAccountCode`) before adding a child — see `account/README.md`.
  Without a shared namespace, a concurrent child-account creation could add
  a child to this exact account between `OpeningBalanceAccountValidator`'s
  `hasChildren` check and the insert, leaving an opening balance on an
  account that's no longer a leaf.
- `getHistory(accountCode)` is every row for that code, oldest first — no
  Envers/`@Audited` needed, since each row already *is* a point-in-time
  revision.
- A row never changes after it's written, so its delta relative to whatever
  preceded it is always reproducible, regardless of when you recompute it.

## Ledger posting is synchronous, and org-scoped

`upsert` computes `delta = newAmount - latest.amount` and, if non-zero,
inserts the new row then posts it with `LedgerPostingService.post` in the same
organization-schema transaction (`OpeningBalancePostingRequests.revised`). A
posting failure, such as a closed fiscal period or an account that cannot take
a posting, rolls the row back and reaches the caller. There is no event, no
processor and no reissue.

`OpeningBalancePostingRequests` is the only class that references
`LedgerSourceType.OPENING_BALANCE`. The account takes its normal side for a
positive delta and the opposite side for a negative one, and
`OPENING_BALANCE_EQUITY` takes the other side. The ledger group of an org-level
posting has no location: `LedgerEntryGroupEntity.sourceLocationId` is nullable,
and `LedgerPostingService.saveLedgerGroup` reads the session location as
nullable.

## Cross-domain account lookups

The account domain's real lookup path (`AccountCache`, `AccountDto`,
`AccountType`) is internal, not `api/`. `OpeningBalanceService.upsert` calls
`account/api/AccountDataFetcher.getByCode` directly — a lean, single-account
lookup added alongside this feature specifically to avoid pulling in
`AccountService.getAll()` + `AccountResponseBuilder`'s full rolled-up tree
just to validate one account.
The resolved, non-null `AccountLookupDto` then goes through
`OpeningBalanceAccountValidator.requireLeafActive`, a stateless `object`
(not a Spring bean, takes the non-null DTO, no not-found concern) that
throws on inactive/has-children. It's a plain object rather than a `@Component`, so it adds no edge to the
Spring graph.

`AccountDataFetcher` only exposes the raw lookup (`AccountLookupDto`:
`accountIsActive`, `hasChildren`, `normalBalanceEntryType`), backed by two
targeted `AccountRepository` queries (`findByCode`, `existsByParentAccountCode`)
rather than the cached full account list; the "must be a leaf/active account
to have an opening balance" rule stays here, in `opening_balance`, since
it's this domain's business rule, not a general fact about accounts.
`normalBalanceEntryType` is exposed on the DTO (instead of the internal
`AccountType`) so this domain never has to import it. Posting direction
flips when a correction's `delta` is negative
(`OpeningBalancePostingRequests`).

This dependency only runs one way: `opening_balance` → `account/api`. The
account domain also needs opening-balance amounts for its own
`AccountResponseDto.openingBalance` rollup, via
`OpeningBalanceReader.getAmountsByAccountCodes(codes)` (latest row per
code, via `OpeningBalanceRepository`'s `SELECT DISTINCT ON` query). The reader
depends only on the repository and is a separate bean from
`OpeningBalanceService`, which depends on `LedgerPostingService`, which depends
on `AccountService` → `AccountResponseBuilder`: `AccountResponseBuilder` calling
`OpeningBalanceService` would close that loop into a Spring circular-bean
dependency. See `account/README.md` for the full explanation.

## REST surface

`OpeningBalanceController` (`secured/opening-balances`):

- `POST` (body: `OpeningBalanceUpsertDto` — `accountCode`, `newAmount`) → `OpeningBalanceService.upsert`.
- `GET history?accountCode=` → `OpeningBalanceService.getHistory`.

Both are thin delegators; all validation/locking/posting logic lives in `OpeningBalanceService`.
