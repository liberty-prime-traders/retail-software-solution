# expense (shared)

Money spent that is not the cost of goods: wages, rent, utilities, freight,
repairs. Pure P&L; nothing here changes FIFO batch cost. Every expense endpoint
requires the `CHART_OF_ACCOUNTS` feature.

Expenses live in two schemas with identical rules: `locations/business/expense/`
(per location) and `organizations/business/org_expense/` (acquisitions and
stock-transfer freight, which no single location owns). This package holds
everything those two share, and sits under `cross_tier/` for the same reason
`cross_tier/authority/` does: `ArchitectureTest`'s isolation rule only covers
`<tier>/business/<domain>` packages. Expense types are their own domain,
`organizations/business/expense_type/`.

## Layout

| Package | Holds |
|---|---|
| (root) | `ExpenseSourceType` |
| `entities` | `@MappedSuperclass` bases (`ExpenseBase`, `ExpenseBatchBase`, `ExpensePaymentBase`, `ExpensePaymentVoidBase`, `ExpenseVoidBase`) and `ExpenseSourceTypeConverter` |
| `repository/` | `@NoRepositoryBean` bases the tier repositories extend |
| `store/` | `ExpenseStore` (the port) and `JpaExpenseStore` (shared reads; tiers only build and save new rows) |
| `operation/` | `ExpenseOperations` (creation, expense voids, reads, expense reissue), `ExpensePaymentOperations` (bulk payments, payment voids, settlements, payment reissue), `ExpenseLookup` (lookups and guards both share), `ExpenseRowResolver` (per-row validation), `ExpenseReissuer` (implemented by each tier's service) |
| `api/`, `record/` | request and response DTOs, `ExpenseResponseBuilder`, and the plain records stores return |
| `kafka_handler/` | the four `EventReissueHandler`s and `ExpenseReissuerResolver` |

Each tier owns only its thin entities (`@Entity @Table @HasReference` over a
base), its repositories, a store (`LocationExpenseStore`, `OrgExpenseStore`)
and a service holding the source-document gates. Both schemas use the same
column names, which is what lets one set of mapped superclasses serve both.

## Model

- `expense_batch` — a grouping and a label. Standalone and wages batches are new per
  submission. Contextual batches (purchase, sale, transfer) carry `source_type` /
  `source_reference`, and a submission appends to the existing batch for that source.
  A partial unique index on `(source_type, source_reference)` backs this;
  `ExpenseStore.lockSourceDocument` serializes find-or-create so the loser of a race
  never hits the index.
- `expense` — immutable: one amount, one payee, one expense type. `expense_account_code` is
  copied from the type at record time and is what every later posting uses, since an admin can
  re-point the type's account afterwards. `source_type` is never
  null; `source_reference` is null exactly for `ADHOC` and `WAGES`.
- payments, payment voids and expense voids — append-only; a void is its own row. A payment
  copies its method's `payment_method_account_code` at record time, and the payment void and
  every reissue post to that copy, never to the method's current account, so re-pointing a
  payment method cannot split a payment and its reversal across two accounts. There
  are no stored status or paid totals: status, `amountPaid` and balance are derived on
  read from payments that have no void row (`ExpenseResponseBuilder`, using
  `PaymentStatusResolver`).
  Every `ExpenseSummaryResponse` carries its expense's payments (voided ones marked), since
  every list is loaded with its payments already.

## Rules

`ExpenseRowResolver` runs before any write, for every row: positive amount, the expense
type exists and lists the request's source type in `eligibleSourceTypes`, the payee
exists and shares at least one contact type with `eligiblePayeeTypes`, and the fiscal
period is open for the expense date (and the payment date when settling). The location
and org standalone screens both store `ADHOC`, so a type cannot be limited to one.

- A settlement on a row pays that row's full amount in the same transaction, through
  `ExpensePaymentOperations.settle`. Later payments are recorded in bulk: one request carries
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
  batch, because there the batch is the unit the user created.
- A payment method with no account code is rejected (supplier and sale payments skip the
  ledger silently in that case; expenses do not, because the liability would stay
  un-debited while the expense shows settled).
- Voiding an expense requires every payment on it to be voided first. Voids check the
  fiscal period for today, and the reversing entry posts on that same date.
- Voiding or cancelling a source document does not touch its expenses and is not blocked
  by them; the money was spent either way.
- `ExpenseStore.lockExpense` is taken before payments and voids.

## Gotchas

- `ExpenseOperations` is not schema-bound; callers run it inside their own schema's
  transaction and pass their store. `ExpenseStore` is the one seam between the schemas:
  storage, the advisory lock, and the event's source context are the only things that
  differ, so they are the only things a tier supplies.
- Reissue is the exception to "callers pass their store": the handlers have no schema in
  hand, so `ExpenseReissuerResolver` picks the tier's service (`ExpenseReissuer`) by
  whether the session has a location, and that service opens its own read-only
  transaction.
- Tier stores are Spring beans with a class-level transaction annotation, so every
  member of `JpaExpenseStore` that they inherit must be open (override members are).
- `@Converter(autoApply = true)` is not relied on anywhere here; every converted column
  has an explicit `@Convert`.

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
