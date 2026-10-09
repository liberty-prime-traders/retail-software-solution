# expense (shared)

Money spent that is not the cost of goods: wages, rent, utilities, freight,
repairs. Pure P&L; nothing here changes FIFO batch cost. Every expense endpoint
requires the `CHART_OF_ACCOUNTS` feature.

Expenses live in two schemas with identical rules: `locations/business/expense/`
(per location) and `organizations/business/org_expense/` (stock-transfer
freight and standalone costs that no single location owns). This package holds
everything those two share, and sits under `cross_tier/` for the same reason
`cross_tier/authority/` does: `ArchitectureTest`'s isolation rule only covers
`<tier>/business/<domain>` packages. Expense types are their own domain,
`organizations/business/expense_type/`.

## Layout

| Package | Holds |
|---|---|
| (root) | `ExpenseSourceType`, `ExpenseTier` (the seam between the two schemas, see Gotchas) |
| `model/` | what the operations read and write: the `*Dto`s stores return (`ExpenseDto`, `ExpensePaymentDto`, ...), `ExpenseAggregate` (and the paid/voided rules over it), and the write-side inputs (`ExpenseSubmission`, `SourceDocument`, `NewExpense`, `PaymentInstruction`, ...) |
| `request/`, `response/` | REST request bodies, `ExpenseSummaryResponse` and `ExpenseResponseBuilder`. Both depend on `model/`, never the reverse |
| `entities/`, `repository/` | `@MappedSuperclass` bases and `@NoRepositoryBean` bases the tier entities and repositories extend |
| `store/` | `ExpenseStore` (persistence only), `JpaExpenseStore` (shared reads; tiers only build and save new rows), `ExpenseDtoMapper`, `ExpensePaymentStateMaintainer` |
| `operation/` | `ExpenseOperations` (creation, expense voids), `ExpenseReadOperations` (single, by-source and recent reads), `ExpensePaymentOperations` (bulk payments, payment voids, settlements), `ExpenseReissueOperations` (re-publishes the four ledger events for existing rows), `ExpenseEvents` (the four ledger events), `ExpenseLookup`, `ExpenseRowResolver` (per-row validation), `ExpenseOperationsFactory`, `ExpenseReissuer` (implemented by each tier's service) |
| `search/` | Advanced search: filters, SQL, validator, summary, `ExpenseSearchFetcher` and `ExpenseSearchOperations` (see Search) |
| `kafka_handler/` | the four `EventReissueHandler`s and `ExpenseReissuerResolver` |

Each tier owns only its thin entities (`@Entity @Table @HasReference` over a
base), its repositories, a store (`LocationExpenseStore`, `OrgExpenseStore`), an
`ExpenseTier` (`LocationExpenseTier`, `OrgExpenseTier`) and a service holding the
source-document gates. Both schemas use the same column names, which is what lets one set of
mapped superclasses serve both.

## Model

- `expense_batch` — a grouping and a label. Standalone and wages batches are new per
  submission. Contextual batches (purchase, sale, transfer) carry `source_type` /
  `source_reference`, and a submission appends to the existing batch for that source.
  A partial unique index on `(source_type, source_reference)` backs this;
  `ExpenseTier.lockSourceDocument` serializes find-or-create so the loser of a race
  never hits the index.
- `expense` — immutable: one amount, one payee, one expense type. `expense_account_code` is
  copied from the type at record time and is what every later posting uses, since an admin can
  re-point the type's account afterwards. `source_type` is never
  null; `source_reference` is null exactly for `ADHOC` and `WAGES`.
- payments, payment voids and expense voids — append-only; a void is its own row. A payment
  copies its method's `payment_method_account_code` at record time, and the payment void and
  every reissue post to that copy, never to the method's current account, so re-pointing a
  payment method cannot split a payment and its reversal across two accounts. Status,
  `amountPaid` and balance come from the one-to-one payment state row (see Payment state);
  `ExpenseResponseBuilder` never recomputes them from payments, and fails when the row is missing.
  Every `ExpenseSummaryResponse` carries its expense's payments (voided ones marked), since
  every list is loaded with its payments already.

## Rules

`ExpenseRowResolver` runs before any write, for every row: positive amount, the expense
type exists and lists the request's source type in `eligibleSourceTypes`, the payee
exists and shares at least one contact type with `eligiblePayeeTypes`, and the fiscal
period is open for the expense date (and the payment date when settling). The location
and org standalone screens both store `ADHOC`, so a type cannot be limited to one.

- A settlement on a row pays that row's full amount in the same transaction, through
  `ExpensePaymentOperations.settleNewExpenses`, which saves every settled row's payment in one
  `savePayments` call, publishes one event each and refreshes their states once, after all the
  expense events. Later payments are recorded in bulk: one request carries
  a collection (at most `ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST`), each carrying its own
  required amount. Requests are grouped by expense reference and each group is
  checked as a whole against the remaining balance, so the error names the total requested and
  by how much it exceeds. All expenses are locked up front in id order so two bulk requests cannot
  deadlock, nothing is saved until every group passes, and the payments of the whole request are
  then saved in one `savePayments` batch with one event each. The response is one expense, with
  its payments, per distinct expense, in order of first appearance.
- A source document (purchase, sale, stock transfer) has one batch that callers never see:
  submissions against it answer with the expenses they wrote, and `by-source` returns every
  expense on the document, oldest first. Only standalone and wages submissions answer with a
  batch, because there the batch is the unit the user created. `by-source` is unpaged by design: a
  document realistically carries ten expenses or fewer, so it loads the batch's whole history.
  Revisit with paging only if that stops holding.
- A payment is not tied to its expense's date: an expense can be prepaid or paid long after, and
  the only date rule is that the payment date's fiscal period is open.
- A payment method with no account code is rejected (supplier and sale payments skip the
  ledger silently in that case; expenses do not, because the liability would stay
  un-debited while the expense shows settled).
- Voiding an expense requires every payment on it to be voided first. Voids check the
  fiscal period for today, and the reversing entry posts on that same date.
- Voiding or cancelling a source document does not touch its expenses and is not blocked
  by them; the money was spent either way.
- `ExpenseTier.lockExpense` is taken before payments and voids.
- `ExpenseStore.refreshPaymentStates` is called after every payment write (`recordPayments`,
  `settleNewExpenses`, `voidPayment`); no triggers. `recordPayments` and `voidPayment` run inside the
  lock they took; `settleNewExpenses` takes none because the expense was written in the same
  transaction and no other transaction can reference it yet.

## Gotchas

- `ExpenseOperations`, `ExpensePaymentOperations`, `ExpenseReadOperations` and `ExpenseReissueOperations` are plain classes bound to one `ExpenseTier` at
  construction (`ExpenseOperationsFactory.operationsFor`); each tier's service builds its own and
  runs them inside its schema's transaction. The tier is the one seam between the schemas: storage,
  the advisory lock and the event's source context are the only things that differ, so they are the
  only things it supplies.
- The handlers have no schema in hand, so `ExpenseReissuerResolver` picks the tier's service
  (`ExpenseReissuer`) by whether the session has a location, and that service opens its own
  read-only transaction.
- Tier stores are Spring beans with a class-level transaction annotation, so every
  member of `JpaExpenseStore` that they inherit must be open (override members are).
- `@Converter(autoApply = true)` is not relied on anywhere here; every converted column
  has an explicit `@Convert`.
- Shared operations call organization-schema services (expense types, contacts, payment methods)
  from inside a location transaction. That is sound because each schema has its own transaction
  manager: the call runs in a separate transaction on a separate connection, so it sees committed
  organization data and is never atomic with the location write. Those lookups are read-only
  admin data, so neither matters here; do not route a write through that path.

## Ledger

Four events (`ExpenseRecordedEvent`, `ExpenseVoidedEvent`, `ExpensePaymentRecordedEvent`,
`ExpensePaymentVoidedEvent`), published by `ExpenseOperations` for both schemas. Their
`sourceContext` is `LocationLevel` or `OrgLevel`, from the store. The reissue handlers
pick the tier from the session (a location in session means a location expense);
`EventRetryService` resets the session location to the log row's `sourceLocationId` for
every retry so a leftover location never misroutes an org-level reissue. The period check is synchronous, but the ledger group is created at consume time, so a
period closed in between fails the posting after the expense has committed — the same
as sale and purchase. See `organizations/business/ledger/README.md`.

## Amounts and dates

Money is `numeric(19, 4)` and rounded with `Decimals.roundToScale4`. Expense dates are
`date`; payment dates are `timestamptz` at the start of the day in the organization zone.

## Payment state

`expense` is immutable, so the paid status the search filters on lives in a one-to-one mutable row
per tier (`expense_payment_state`, `org_expense_payment_state`; `ExpensePaymentStateBase`:
`payment_status`, `amount_paid`, optimistic `version`). It is not audited: every input is an
immutable, already-audited row.

- Each tier's `saveNewExpense` creates the row as `UNPAID` / 0 in the same save.
- `ExpensePaymentStateMaintainer.refresh` recomputes `amount_paid` from active (non-voided)
  payments (`sumActivePaidByExpenseId`, JPQL per tier) and the status with `PaymentStatusResolver`.
  It throws when an expense has no state row: search joins the state table, so a missing row would
  hide the expense from search and summary.
- Voiding an expense does not touch the row: it requires every payment voided first, so it already
  reads `UNPAID` / 0. "Voided" is never a `PaymentStatus`; it is derived from the void table.
- Reads take status and `amountPaid` from this row: `JpaExpenseStore` loads the rows into
  `ExpenseAggregate.paymentStates` and `ExpenseResponseBuilder` uses them as is. Write flows
  (`recordPayments`, `voidPayment`, new expenses) reload the aggregate after
  `refreshPaymentStates` before building their response, so a response never shows an in-memory
  patch that could differ from the row.
- A response is built once per request over the whole aggregate (`buildSummaries`), not once per
  expense: the expense type, contact and payment method lookups are loaded once for the request.

## Search

`ExpenseSearchOperations` is bound to one tier's `ExpenseStore` and `ExpenseSearchFetcher` (executor on its
datasource plus its `ExpenseSearchTables`); each tier's search service builds it through
`ExpenseSearchOperationsFactory`.

- The query returns only `(id, reference_number, created_on)`. The page's ids are loaded in one
  batch (`loadForExpenses`) and shaped by `ExpenseResponseBuilder`, so rows are the same
  `ExpenseSummaryResponse` as every other read. Query order is kept; the builder's is not relied on.
- The expense table joins its state table 1:1 and LEFT JOINs the void table; voided means a void row
  exists. `voided` is a tri-state filter independent of `paymentStatuses`.
- `paymentMethodIds` is an `EXISTS` over active payments, never a join, so an expense paid with
  several matching methods is one row.
- `sourceReferences` match `source_reference` alone; references carry unique prefixes, so there is no
  source-type filter.
- Paging is keyset on `(created_on, id)` descending, backed by `idx_expense_created_on_id` /
  `idx_org_expense_created_on_id`; one extra row is fetched to compute `hasMore`.
- `createdFrom` and `createdBefore` are both required on list and summary searches, so every query is
  time-bounded. Every `*Before` bound is exclusive: `expenseDateBefore = 10-08` excludes 10-08, and
  equal bounds select nothing. Amount bounds reject negatives; zero is allowed.
- Reference lists (`expenseReferenceNumbers`, `sourceReferences`) drop null and blank entries
  (`ExpenseSearchParameters.sanitized`, built on `StringUtils.dropBlank`) before validation, so blanks
  neither count toward the size caps nor reach the query. The sale, sale payment, purchase and tax
  entry searches do the same for their reference lists.
- The summary groups by expense type, voided flag and stored status. `byStatus` holds the four
  `ExpenseSummaryBucket`s (`UNPAID`, `PARTIAL`, `PAID`, `VOIDED`), always all four; the bucket comes
  from the voided flag first, then the stored status. `byExpenseType` is flat: each entry carries live
  count / amount / paid / outstanding plus `voidedCount` / `voidedAmountTotal`, and only types with a
  matching expense appear. Overall totals exclude voided, reported as `voidedCount` /
  `voidedAmountTotal`. The SQL forces paid and outstanding to 0 for voided expenses instead of reading
  the state row. That is correct because voiding an expense requires every payment on it to be voided
  first, so a voided expense always reads `UNPAID` / 0.
