# account

Owns the organization's chart of accounts: a tree of `AccountEntity` rows keyed
by dotted `code` (e.g. `001.001.001`), each carrying a running `currentBalance`
that `ledger` postings increment directly (no ledger-entry read needed to show
a balance).

## Domain

- `AccountEntity` — `code` (unique, not updatable), `name`, `accountType`,
  `currencyCode`, `accountIsActive`, `accountIsSystemMaintained`,
  `currentBalance` (running total, scale 4), `balanceUpdatedAt`,
  `parentAccountCode`. Audited; most columns are `@NotAudited` since they're
  either immutable or a hot-path running total not worth versioning.
- `AccountDto` — internal domain projection produced by `AccountMapper`.
  `label` / `toDisplayCode()` render the code with leading zeros trimmed per
  segment (`001.001` → `1.1`).
- `api/AccountResponseDto` — outbound projection enriched with rolled-up
  `currentBalance`, `parentAccount` (label), `accountIsExtensible`,
  `balanceSignal`, and rolled-up `openingBalance`.

`AccountCache` (`@CacheSchemaLevel(ORGANIZATION)`) is the only path to
`AccountRepository` for anything but balance increments — `getAll()` is
cached and evicted whole on every `create`/`saveAll`/`update`.

- `api/AccountDataFetcher` — the exception to that: a lean, uncached
  single-account lookup (`findByCode` → `AccountLookupDto`: `code`,
  `accountIsActive`, `hasChildren`, `normalBalanceEntryType`) for callers
  that just need to validate one account and don't want the cost of
  `AccountService.getAll()` + `AccountResponseBuilder`'s full rolled-up tree.
  `hasChildren` and the lookup itself are two targeted `AccountRepository`
  queries (`existsByParentAccountCode`, `findByCode`), not a filter over the
  cached list. Used directly by `opening_balance`'s `OpeningBalanceService`
  and `OpeningBalanceHandlerForKafka`.
  `AccountDataFetcher` must **not** depend on `AccountResponseBuilder` —
  that's why `getAll()` lives on `AccountService` instead: `AccountService`
  → `AccountResponseBuilder` → `AccountDataFetcher` is fine (one direction),
  but `AccountDataFetcher` → `AccountResponseBuilder` would close the loop.
  For the same reason, `AccountResponseBuilder`'s `openingBalance` rollup
  calls `opening_balance`'s `OpeningBalanceService.getAmountsByAccountCodes`
  **directly**, not through `AccountDataFetcher` — `OpeningBalanceService`
  itself depends on `AccountDataFetcher` directly (for `upsert`'s
  validation), so routing the amounts call through
  `AccountDataFetcher` too would make `AccountDataFetcher` depend on
  `OpeningBalanceService`, which depends on `AccountDataFetcher` — a cycle.
  Keeping `AccountResponseBuilder` as the caller (not `AccountDataFetcher`)
  avoids it, since nothing in the `OpeningBalanceService` chain depends on
  `AccountResponseBuilder`.

## Account types (`AccountType`)

Five base types plus five "contra" counterparts, each declaring its
`normalBalance` (`EntryType.DEBIT`/`CREDIT`):

| Type      | Normal balance | Contra                   |
|-----------|----------------|--------------------------|
| ASSET     | DEBIT          | ASSET_CONTRA (CREDIT)    |
| LIABILITY | CREDIT         | LIABILITY_CONTRA (DEBIT) |
| EQUITY    | CREDIT         | EQUITY_CONTRA (DEBIT)    |
| REVENUE   | CREDIT         | REVENUE_CONTRA (DEBIT)   |
| EXPENSE   | DEBIT          | —                        |

- `canBeRoot()` — only ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE (not the
  contras) may be created as root accounts (`AccountService.createRoot`).
- `isClosingType()` — REVENUE, REVENUE_CONTRA, EXPENSE; the set zeroed out at
  year-end.
- `balanceSignal()` — maps to `BalanceSignal.MONEY_IN`/`MONEY_OUT` for
  display; overridden to `ZERO_BALANCE`/`IRREGULAR_BALANCE` in
  `AccountResponseBuilder` when the rolled-up balance is zero or negative
  where it shouldn't be.

## System accounts (`api/SystemAccount`)

A fixed chart-of-accounts skeleton (Assets → Current Assets → Cash, etc.),
each entry carrying its own `code`/`name`/`type`/`parent`. `CoaDefaultsInserter`
seeds any `SystemAccount` missing from `AccountCache.getAll()` on org setup —
safe to re-run, since it only inserts codes not already present.

- `accountIsSystemMaintained = true` accounts cannot be renamed, deactivated,
  or renumbered — `AccountService.rename`/`toggleActive` reject them outright.
- `isSingleLevelExtensionPoint()` — only `DIGITAL_PAYMENTS`, `TAX_RECOVERABLE`,
  `TAX_PAYABLE` accept a **single level** of user-created children directly
  beneath them (e.g. a payment account under `DIGITAL_PAYMENTS`) — not
  unlimited depth, and not an inherited trait: a child of one of these
  accounts is not itself an extension point. Every other system account is a
  closed leaf/branch. (Named deliberately to avoid the earlier `isExtensible()`,
  which read as if extensibility propagated down the tree — it doesn't.)
- `ChildAccountCreator` enforces both halves of that: `ensureParentCanGainChild`
  blocks creating a child under an inactive parent, or under a
  system-maintained parent that isn't a single-level extension point;
  `preventSystemAccountGainingGrandChild` separately blocks creating a child
  whose *parent's own parent* (grandparent) is one — that's what actually
  caps the depth at one level (you can add "Bank" under `DIGITAL_PAYMENTS`,
  but not a child of "Bank"). `AccountResponseBuilder.canGainChildren`
  mirrors that second rule for display (`accountIsExtensible` on the
  response DTO): an account can gain children unless its own parent is a
  single-level extension point.

## Creating accounts

- **Root** (`AccountService.createRoot`) — type must satisfy
  `canBeRoot()`; code is `AccountCodeGenerator.generateRootCode` (next free
  3-digit code ≥ 100 among existing roots, since 001–005 are reserved for the
  `SystemAccount` skeleton).
- **Child** (`AccountService.createChild` → `ChildAccountCreator`) — inherits
  the parent's `accountType`/`currencyCode`; code is
  `AccountCodeGenerator.generateChildCode` (`parent.code + ".NNN"`, next free
  3-digit segment, max 999 siblings). Validations, in order:
  - parent must be active and (if system-maintained) extensible,
  - no active/pending sibling with the same name (case-insensitive),
  - parent's own parent, if a system account, must not itself be extensible
    (blocks a 3rd-level insert under an extensible system branch — e.g. you
    can add a payment account under Digital Payments, but not a child of
    that payment account),
  - `AccountUsagesFinder.failOnUsagesForCode(parent.code)` — a parent already
    referenced by a payment method or tax type cannot gain children.

  Before any of that, `AccountService.createChild` takes
  `OrgEntityAdvisoryLock.acquire(LockNamespaces.ACCOUNT, parentAccountCode)`.
  `opening_balance`'s `OpeningBalanceService.upsert` takes the same lock on
  the same namespace before its own `hasChildren` check — without sharing
  it, a child could be added to an account concurrently with (and
  interleaved between) that check and an opening balance being posted to
  it, leaving a non-leaf account with an opening balance.

## Usages (`api/AccountUsageProvider`)

Other domains that reference an account code by business meaning implement
this interface so `AccountUsagesFinder` can report/forbid conflicting edits:

- `PaymentMethodAccountUsageProvider` (`payment_method/`)
- `TaxAccountReferenceProviders` (`org_jurisdiction_tax_type/`)

`AccountUsagesFinder.findUsagesForAccountCode` fans out to every provider and
returns the non-empty ones; `failOnUsagesForCode` throws if any exist.

## Validators

- `PaymentAccountValidator` — a payment method's linked account must be
  active, `ASSET`, and either `SystemAccount.CASH` or a child of
  `DIGITAL_PAYMENTS` (system) / any active non-system asset (org-defined).
- `TaxAccountsValidator` — payable account must be `LIABILITY` (or a child of
  `TAX_PAYABLE`); recoverable account is required iff
  `TaxRecoveryType.RECOVERABLE` and must be `ASSET` (or a child of
  `TAX_RECOVERABLE`).

## Balances

- `AccountEntity.currentBalance` is a running total updated in place via
  `AccountRepository.incrementBalance` (`UPDATE ... SET current_balance =
  current_balance + :delta`) — never read-modify-write through the cached DTO.
- `AccountService.patchBalances(entries: List<LedgerEntrySummaryDto>)` is
  called by `ledger` after posting; the delta is applied as-is when the
  entry's `EntryType` matches the account's `normalBalance`, negated
  otherwise.
- `AccountResponseBuilder.buildResponse` rolls balances up the tree at read
  time (`computeRolledUpAmount`) — a parent's displayed balance is the sum
  of its leaf descendants' `currentBalance`, not its own row (accounts with
  children never post directly). `openingBalance` is rolled up the same way,
  sourced via `opening_balance`'s `OpeningBalanceService.getAmountsByAccountCodes`
  (see `api/AccountDataFetcher` above for why this calls `OpeningBalanceService`
  directly rather than through `AccountDataFetcher`).
- Year-end close (`api/DenormalizedYearEndBalanceTransfer`) zeroes every
  `isClosingType()` account with a non-zero balance and transfers net income
  (revenue − contra − expense) into `SystemAccount.RETAINED_EARNINGS`, all via
  direct `incrementBalance` calls.
  - **Known gap** (see TODO in the class): this mutates running balances
    without writing offsetting ledger entries. A proper close should post a
    `ledger_entry_group` with `source_type = YEAR_END_CLOSE` instead.

## Selection trees (`api/AccountTreeBuilder`)

Builds three `TreeNode<String>` forests off the same account list for
different UI pickers — `payable`, `recoverable`, `paymentMethods` — pruning
any branch that is neither selectable nor an ancestor of a selectable node.
Selectability mirrors the validators above (e.g. `paymentMethods` accepts
`CASH`, system children of `DIGITAL_PAYMENTS`, or any active non-system
asset).
