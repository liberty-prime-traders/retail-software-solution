# expense (location)

Location-schema expenses. The rules, model and ledger behavior are shared with the
org-level expenses and documented in `cross_tier/expense/README.md`; this package
holds only what is location-specific.

- Tables `expense_batch`, `expense`, `expense_payment`, `expense_payment_void`,
  `expense_void` and `expense_payment_state` are in the location schema, so `expense_type_id` and `payee_contact_id`
  are plain UUIDs (contact and expense type live in the organization schema).
- `LocationExpenseStore` locks through `EntityAdvisoryLock` and publishes events with a
  `LocationLevel` source context.
- `ExpenseService` holds the source-document gates and request mapping:
  - Purchase: not `DRAFT`. Allowed from `ORDERED` onward, including `CANCELED`. The
    payee defaults to the purchase's supplier.
  - Sale: `CONFIRMED` or `VOIDED`. `DRAFT` and `DISCARDED` are rejected.
  - Wages: the type is fixed to the seeded wage type and the batch description is
    generated.

## Payment state and search

`expense_payment_state` and the search are shared with the org tier and documented in
`cross_tier/expense/README.md`. Location specifics:

- `ExpensePaymentStateEntity` / `ExpensePaymentStateRepository` (the JPQL targets
  `ExpensePaymentEntity` / `ExpensePaymentVoidEntity`); `LocationExpenseStore` creates the row in
  `saveNewExpense` and delegates `refreshPaymentStates` to `ExpensePaymentStateMaintainer`.
- `ExpenseSearchFetcher` binds the shared query builder to `ExpenseSearchTables.LOCATION` and the
  location datasource; `ExpenseSearchService` runs `ExpenseSearchOperations` read-only on the
  location schema (`POST secured/expenses/search` and `.../search/summary`).
- `ExpensePaymentStateConsistencyTest` pins the stored state to what `ExpenseResponseBuilder`
  derives from the payments.
