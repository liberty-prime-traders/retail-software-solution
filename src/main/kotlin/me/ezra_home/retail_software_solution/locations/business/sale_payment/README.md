# sale_payment

Money received against a sale. Location schema. The sale package's `README.md` (section 8, Payments) describes how the
sale and sale-session flows call into this package; this document covers the payment rows themselves.

## Model

- `sale_payment` and `sale_payment_void` are append-only. A payment is never edited; a void is one row per payment
  (unique on `sale_payment_id`) carrying a reason. There is no stored paid amount or balance on the sale, so the paid
  amount is always derived from the rows.
- A payment is **active** when it has no `sale_payment_void` row. That rule is written once, in the `active_sale_payment`
  view (`ViewNames.ACTIVE_SALE_PAYMENT`, `views/active_sale_payment.yml`), exposed as the read-only `ActiveSalePaymentEntity`
  through `ActiveSalePaymentRepository`.
- Everything that needs "paid so far" reads that view: `SalePaymentFetcher.calculatePaidAmount` / `calculatePaidAmounts` /
  `hasActivePayments`, and the paid sum in `SaleSearchQueryBuilder`. None of them filters voids in memory.
- The view selects only the columns those readers need (`id`, `reference_number`, `sale_id`, `amount`, `created_on`). A
  reader that needs another column adds it to the view's changeset.

## Where voided rows are still read directly

`SalePaymentFetcher.getPaymentSnapshots` and the payment search list voided payments with their reason, so they read
`sale_payment` and join `sale_payment_void`. The payment search summary splits active and voided totals from that same
join. A change to what counts as voided therefore touches the view and these joins.

## Recording and voiding

- `SalePaymentService.recordPayment` requires an open fiscal period for the payment date, takes the sale lock through
  `SaleDataFetcher.lockAndGetSaleContext`, checks the sale accepts payments and that the amount does not exceed the
  remaining balance, then hands the row to `SalePaymentWriter.write`. The writer saves it, publishes the Kafka event and
  returns the new status from `PaymentStatusResolver.resolve`; the service stores that status on the sale.
- `SalePaymentService.voidPayment` requires an open fiscal period for today, rejects an already-voided payment and a voided
  sale, takes the same sale lock, saves the void row, recomputes the paid amount and status, stores the status and
  publishes the void event through `SalePaymentVoidHandlerForKafka`.
- Balance rules live in `SalePaymentValidator`; the session flow reuses them. The amount is a `PositiveMoney`, so positivity is checked when the request binds.
- Ledger postings are produced by the Kafka consumers of the recorded and voided events, not here.
