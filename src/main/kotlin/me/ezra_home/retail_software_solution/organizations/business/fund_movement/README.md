# fund_movement

Money moved between the organization's own ledger accounts, and owner capital and draws. Organization schema; every
endpoint requires the `CHART_OF_ACCOUNTS` feature (`secured/fund-movements`).

## Types (`api/FundMovementType`)

| Type | User picks | Debit | Credit |
|---|---|---|---|
| `TRANSFER_BETWEEN_ACCOUNTS` | `fromAccountCode`, `toAccountCode` | `to` | `from` |
| `CONTRIBUTE_CAPITAL_TO_BUSINESS` | `accountCode` | the account | `OWNERS_CAPITAL` |
| `WITHDRAW_FOR_PERSONAL_USE` | `accountCode` | `OWNERS_DRAWS` | the account |

The enum names are the API values. The equity side is fixed by type and never appears in a request. One route per
type (`transfer`, `capital`, `draw`) so each can carry its own static authorization expression.

## Model

- `fund_movement` and `fund_movement_void` are immutable and append-only, so neither is audited. A void is its own row,
  unique per movement (`fund_movement_id`), and carries `void_date` (always today in the organization zone, never
  user-supplied) and an optional reason. Corrections are void and re-enter; there is no update.
- Direction is stored: `debit_account_code` and `credit_account_code` are written at record time, and posting and voiding
  read them, never the type.
- References are `FMOV` and `FMVD` prefixed. `FundMovementResponse` carries the movement's `id` because the void route
  takes it.

## Rules

- Validation is in `FundMovementService`, not on the DTOs. Amount is a `PositiveMoney` (zero, a negative, more decimals than the session money scale, or a magnitude above
  `Decimals.MAXIMUM_TRANSACTION_AMOUNT` are rejected when the request binds, never rounded); `movementDate` must not be after today in the organization zone; a transfer needs two different accounts; the note and the void reason are blank-to-null and unbounded (`text`).
- Every user-picked account goes through `FundMovementAccountValidator`, which calls
  `AccountSelectionService.requireSelectable(AccountSelectionRule.FUND_MOVEMENT, code)` in sorted code order, so two
  movements over the same pair take the account locks in the same order.
- The open fiscal period and account postability are enforced by `LedgerPostingService.post`, not repeated here.
- A void posts as a reversal, so a movement on a since-deactivated account can still be voided. Movements are reported to
  `AccountUsagesFinder` by `FundMovementAccountUsageProvider` (voided ones included, newest ten references), which stops
  an account with movements from gaining children (a void against a non-leaf account is rejected) or being deactivated.
- `debit_account_code` and `credit_account_code` are foreign keys to `account.code`.
- A void takes `OrgEntityAdvisoryLock` on the movement id (`LockNamespaces.FUND_MOVEMENT`) before checking for an existing
  void row.

## Ledger posting is synchronous

Unlike the event-driven posting of the other domains, the service saves the row and then calls
`LedgerPostingService.post` in the same organization-schema transaction. A posting failure rolls the row back and reaches
the caller. There are no Kafka events, processors or reissue handlers. `FundMovementPostingRequests` builds both requests;
a void posts `FUND_MOVEMENT_VOID` with the legs swapped on `void_date`. See `ledger/README.md`.

## Responses and search

- Responses are ready to render: account `code` and `label`, the recorded-by name, and `void` (null, or `voidDate`,
  `reason`, `voidedBy`). `FundMovementResponseBuilder` resolves labels in one pass per request.
- `POST search` follows `cross_tier/expense/README.md`: keyset on `(created_on, id)` descending (`idx_fund_movement_created_on_id`),
  one extra row for `hasMore`. The query selects the full movement and void columns, `FundMovementSearchRowMapper` builds the
  DTOs, and the response builder resolves labels and names for the page.
  `createdFrom` / `createdBefore` are required, the upper bound is exclusive and the span is at most one year.
- Filters: `types`, `accountCodes` (either side, a single-row predicate so a movement is never duplicated), `movementDateFrom` /
  `movementDateBefore`, `statuses` (both or neither means no filter) and `references` (blanks dropped).
