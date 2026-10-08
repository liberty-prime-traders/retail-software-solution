# expense (location)

Location-schema expenses. The rules, model and ledger behavior are shared with the
org-level expenses and documented in `cross_tier/expense/README.md`; this package
holds only what is location-specific.

- Tables `expense_batch`, `expense`, `expense_payment`, `expense_payment_void` and
  `expense_void` are in the location schema, so `expense_type_id` and `payee_contact_id`
  are plain UUIDs (contact and expense type live in the organization schema).
- `LocationExpenseStore` locks through `EntityAdvisoryLock` and publishes events with a
  `LocationLevel` source context.
- `ExpenseService` holds the source-document gates and request mapping:
  - Purchase: not `DRAFT`. Allowed from `ORDERED` onward, including `CANCELED`. The
    payee defaults to the purchase's supplier.
  - Sale: `CONFIRMED` or `VOIDED`. `DRAFT` and `DISCARDED` are rejected.
  - Wages: the type is fixed to the seeded wage type and the batch description is
    generated.
