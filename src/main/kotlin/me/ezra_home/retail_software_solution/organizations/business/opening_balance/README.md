# opening_balance

Lets org admins declare a starting balance for any postable (leaf, active)
account. `opening_balance` rows are **append-only** — a correction inserts a
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
- It makes Kafka replay safe (see below): a row never changes after it's
  written, so its delta relative to whatever preceded it is always
  reproducible, regardless of when you recompute it.

## Ledger posting is async, and org-scoped

`upsert` computes `delta = newAmount - latest.amount` and, if non-zero,
inserts the new row then publishes `OpeningBalanceUpsertedEvent` via
`OpeningBalanceHandlerForKafka.publish` — the actual `LedgerEntryGroup` post
happens later, in `ledger/processors/OpeningBalanceAccountingProcessor`, via
the normal Kafka transaction-event pipeline.

This required two changes to shared Kafka infra, because that pipeline was
previously location-only:

- `TransactionEvent.sourceContext` was widened from
  `EventSourceContext.LocationLevel` to the base `EventSourceContext`
  (`OrgLevel` already existed for the unrelated `catalog` event pipeline).
  `OpeningBalanceUpsertedEvent` is the first `TransactionEvent` to use
  `OrgLevel` — there's no location, this is an org-level admin action.
- `TransactionEventProducer` / `DltPublisher` computed their Kafka partition
  key by reading `sourceContext.locationSchema` directly; both now branch on
  `LocationLevel`/`OrgLevel` to pick `locationSchema`/`orgSchema`.
- `LedgerPostingService.saveLedgerGroup` used to call
  `SessionContextProvider.getLocationId()` unconditionally (throws with no
  location in session) — changed to the nullable
  `SessionContextProvider.getSession().location?.id`, matching
  `LedgerEntryGroupEntity.sourceLocationId: UUID?`, which was already
  nullable. `LedgerEntryGroupRepository` gained
  `existsBySourceReferenceNumberAndSourceLocationIdIsNull` so the idempotency
  check works for a null location too.

`LedgerSourceType.OPENING_BALANCE` stays **internal** to the `ledger` domain
on purpose — only `OpeningBalanceAccountingProcessor` (same domain)
references it. `opening_balance` never imports anything from `ledger` at
all; it only ever emits the event.

## Reissue (`OpeningBalanceHandlerForKafka.reissue`)

The generic DLT reissue contract assumes one event per **immutable** source
document, rebuilt from current DB state. That's exactly what append-only
buys here: `reissue(sourceDocumentId)` loads that *specific* row (never
mutated since), finds the row immediately before it for the same account by
`created_on`, and recomputes `delta = row.amount - previous.amount` — this
is deterministic no matter how much later the DLT retry runs, unlike trying
to infer "the delta that failed" from a mutable "current balance" row.

## Cross-domain account lookups

The account domain's real lookup path (`AccountCache`, `AccountDto`,
`AccountType`) is internal, not `api/`. `OpeningBalanceService.upsert` and
`OpeningBalanceHandlerForKafka.reissue` each call
`account/api/AccountDataFetcher.findByCode` directly — a lean, single-account
lookup added alongside this feature specifically to avoid pulling in
`AccountService.getAll()` + `AccountResponseBuilder`'s full rolled-up tree
just to validate one account — and each does its own not-found check
(`?: throw RtsGenericException(...)`) right there, since only the caller
has `accountCode` in scope for that message once the lookup returns null.
The resolved, non-null `AccountLookupDto` then goes through
`OpeningBalanceAccountValidator.requireLeafActive`, a stateless `object`
(not a Spring bean, takes the non-null DTO, no not-found concern) that
throws on inactive/has-children. It's a plain object rather than a shared
`@Component` specifically because `OpeningBalanceService` already depends
on `OpeningBalanceHandlerForKafka` — making the validation a method *on*
`OpeningBalanceService` (as briefly considered) would force
`OpeningBalanceHandlerForKafka` to depend back on `OpeningBalanceService`,
a direct two-bean cycle. A stateless object has no constructor and adds no
edge to the Spring graph either way.

`AccountDataFetcher` only exposes the raw lookup (`AccountLookupDto`:
`accountIsActive`, `hasChildren`, `normalBalanceEntryType`), backed by two
targeted `AccountRepository` queries (`findByCode`, `existsByParentAccountCode`)
rather than the cached full account list; the "must be a leaf/active account
to have an opening balance" rule stays here, in `opening_balance`, since
it's this domain's business rule, not a general fact about accounts.
`normalBalanceEntryType` is exposed on the DTO (instead of the internal
`AccountType`) so this domain never has to import it. Posting direction
flips when a correction's `delta` is negative
(`OpeningBalanceHandlerForKafka.directionFor`).

This dependency only runs one way: `opening_balance` → `account/api`. The
account domain also needs opening-balance amounts for its own
`AccountResponseDto.openingBalance` rollup, via
`OpeningBalanceService.getAmountsByAccountCodes(codes)` (latest row per
code, via `OpeningBalanceRepository`'s `SELECT DISTINCT ON` query) — but
`account`'s `AccountResponseBuilder` calls that directly rather than through
`AccountDataFetcher`, specifically to avoid a Spring circular-bean
dependency: `OpeningBalanceService` already depends on `AccountDataFetcher`
directly, so `AccountDataFetcher` can't depend back on `OpeningBalanceService`
too. See `account/README.md` for the full explanation.

## REST surface

`OpeningBalanceController` (`secured/opening-balances`):

- `POST` (body: `OpeningBalanceUpsertDto` — `accountCode`, `newAmount`) → `OpeningBalanceService.upsert`.
- `GET history?accountCode=` → `OpeningBalanceService.getHistory`.

Both are thin delegators; all validation/locking/posting logic lives in `OpeningBalanceService`.
