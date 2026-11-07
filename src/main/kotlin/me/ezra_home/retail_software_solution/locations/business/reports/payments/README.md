# reports / payments

Read-only Payments screen search over sale and supplier (purchase) payments. Location schema. Raw SQL over `TableNames`; no
entities or repositories, so the package depends on `sale_payment` and `supplier_payment` only through the table layout.

## Payment types

- `PaymentSource` is the one place a payment type is defined: its tables (payment, void, parent record), the parent's
  contact column, its `BalanceEffect` (sales `ADDS`, purchases `SUBTRACTS`). A new type is
  one entry here plus one `PaymentType` value; no query, service or DTO changes.
- Every source's tables must expose the column names the search reads: `id`, `reference_number`, `created_on`,
  `payment_date`, `payment_method_id`, `amount` on the payment, `reason` on the void table, and `reference_number` plus the
  contact column on the parent.
- `active_supplier_payment` has no payment method or payment date, so the queries join the payment and void tables directly.
  A payment is voided when its void row exists.

## Request

- `api/PaymentSearchService` is the entry point (`search` for the list, `summarize` for the per-type totals).
- `types` selects the types; an unchecked type is left out of the response and its query is not run. Totals are never
  combined across types.
- `referenceNumbersByType` filters a type by its parent record's reference number. Setting a list for any type drops the
  selected types that have none (`PaymentSearchParameters.includes`).
- `contactIds` matches a sale's customer, a purchase's supplier or any other source's parent contact.
- `statuses` keeps every row when it is empty or holds both values; a single value adds a void-row clause.

## Response

- The summary is a list with one block per included type, in source order. A payment method appears in a block when it has any
  active or voided payment in range, so a method with only voided payments still shows.
- The list is one `UNION ALL` query with a branch per included source. Each branch applies the keyset cursor
  `(created_on, id)` and its own `LIMIT`, so Postgres reads at most one page per source, and the outer query orders and
  limits the combined rows. Ordering and uuid comparison stay in the database.
- Each row's `referenceNumber` is the payment's own number, so its prefix shows the type.
