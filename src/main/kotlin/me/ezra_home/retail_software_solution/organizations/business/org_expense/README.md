# org_expense (organization)

Organization-level expenses: stock-transfer freight and standalone costs that no single
location owns. Rules, model and ledger behavior are shared with the location expenses
(`cross_tier/expense/README.md`); the differences are:

- Tables `org_expense_batch`, `org_expense`, `org_expense_payment`,
  `org_expense_payment_void`, `org_expense_void` and `org_expense_payment_state` are in the organization schema, so
  `payee_contact_id`, `expense_type_id` and `payment_method_id` are real foreign keys.
- `OrgExpenseTier` is this schema's `ExpenseTier`: it exposes `OrgExpenseStore` for storage, locks through
  `OrgEntityAdvisoryLock` (string keys) and supplies the `OrgLevel` source context, so the consumer
  initializes only the organization session and the ledger group has a null `source_location_id`.
- Source types are `ADHOC` (the org-level standalone screen) and `STOCK_TRANSFER`. A
  transfer is looked up by reference in the organization schema (not filtered by the
  caller's location) and is rejected only while `DRAFT`; `CANCELLED` and `DISPATCHED`
  are allowed; a cancelled transfer may still carry expenses because the money was spent either way.
- Payment state and search follow `cross_tier/expense/README.md`: `OrgExpensePaymentStateEntity` /
  `OrgExpensePaymentStateRepository`, created and refreshed by `OrgExpenseStore`;
  `OrgExpenseSearchService` builds the shared `ExpenseSearchFetcher` over `OrgExpenseSearchExecutor` and
  `ExpenseSearchTables.ORGANIZATION`, and serves `POST secured/org-expenses/search` and
  `.../search/summary`.
