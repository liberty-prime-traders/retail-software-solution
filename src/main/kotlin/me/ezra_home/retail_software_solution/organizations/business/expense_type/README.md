# expense_type (organization)

The admin-managed catalogue of expense categories. A type decides which ledger account an expense
posts to and which payees and entry points may use it. Endpoint: `secured/expense-types`
(`GET` with an optional `sourceType`, `POST`, `PUT`; there is no delete), behind the
`CHART_OF_ACCOUNTS` feature. Expenses themselves are in `cross_tier/expense/README.md`.

## Model

- `name`, `expense_account_code`, `eligible_payee_types` (`ContactType` set) and
  `eligible_source_types` (`ExpenseSourceType` set). Both sets must be non-empty. The account must be
  an active expense leaf account, checked on create and whenever the account changes.
- `code` is null for types an admin created. System types carry their `SystemExpenseType` code, which
  is unique (`uq_expense_type_code`) and never updated. `system_defined` is never updated either.
  Both columns are `@NotAudited`, so `expense_type_aud` has neither.
- An expense copies `expense_account_code` when it is recorded, so editing a type's account never
  re-points expenses already booked.

## Rules

- Names are unique by `StringUtils.isEquivalent` (case and spacing insensitive). No database
  constraint can express that, so create and rename take `OrgEntityAdvisoryLock`
  (`LockNamespaces.EXPENSE_TYPE`, key `name`) and then read every row. Serializing all name changes
  is acceptable because the table is small and rarely written.
- A system-defined type can only be renamed; any change to its account or eligibility is rejected
  (`ExpenseTypeUpdateDto.changesAnythingButName`).
- `ExpenseTypeSeeder` (an `OrgDataSeeder`) inserts every `SystemExpenseType` whose code is not
  present yet, so it is idempotent. Existing organizations receive new system types through the
  organization profile's seed-defaults call.
- Callers resolve a system type with `ExpenseTypeService.getBySystemExpenseType`, which fails with
  "has not been seeded" when the seeder has not run for the organization.
- Eligibility is enforced per expense row by `ExpenseRowResolver`, not here.
- `getAll(sourceType)` returns types ordered by name, filtered to those listing `sourceType` when it
  is given.
