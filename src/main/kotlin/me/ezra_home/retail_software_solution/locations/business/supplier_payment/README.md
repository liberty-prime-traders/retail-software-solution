# supplier_payment

Money paid to a supplier against a purchase, optionally against one delivery of it. Location schema.

## Model

- `supplier_payment` and `supplier_payment_void` are append-only. A payment carries `purchase_id` and an optional
  `delivery_id`; a void is one row per payment (unique on `supplier_payment_id`) with a reason. No paid amount is stored on
  the purchase or the delivery; it is derived from the rows.
- A payment is **active** when it has no void row. That rule is written once, in the `active_supplier_payment` view
  (`ViewNames.ACTIVE_SUPPLIER_PAYMENT`, `views/active_supplier_payment.yml`), exposed as the read-only
  `ActiveSupplierPaymentEntity` through `ActiveSupplierPaymentRepository`.
- `PaymentsCalculatorService` sums that view for a purchase (`calculatePaidAmountForPurchase`) or a delivery
  (`calculatePaidAmountForDelivery`), and the paid-total lateral in `PurchaseSearchQueryBuilder` reads it too. Nothing
  filters voids in memory.
- `SupplierPaymentService.getPaymentsByPurchaseId` lists voided payments with their void, so it reads `supplier_payment`
  and `supplier_payment_void` directly.

## Recording and voiding

- `recordPayment` requires a positive amount (`Decimals.requirePositive`) and an open fiscal period for the payment date,
  then takes the purchase lock. A delivery-level payment cannot push that delivery's paid total above its delivery total.
  Once the purchase is fully delivered, the purchase's paid total cannot exceed the delivered total; before that, the
  ceiling from `PurchasePaymentCeilingService` decides the resulting status. The service saves the row, stores the status from
  `PurchasePaymentStatusService.resolvePaymentStatus` and publishes the Kafka event through
  `SupplierPaymentHandlerForKafka`.
- `voidPayment` requires an open fiscal period for today, takes the purchase lock, rejects an already-voided payment,
  saves the void row, stores the status resolved from the paid total read before the void minus the voided amount
  (`PurchasePaymentStatusService.patchThenReturnPaymentStatusAfterVoid`) and publishes the void event through `SupplierPaymentVoidHandlerForKafka`.
- Ledger postings are produced by the Kafka consumers of the recorded and voided events, not here.
