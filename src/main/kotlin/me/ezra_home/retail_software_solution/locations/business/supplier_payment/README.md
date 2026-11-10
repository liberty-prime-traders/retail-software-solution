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
- `SupplierPaymentDataFetcher.getPaymentsByPurchaseId` lists voided payments with their void, so it reads `supplier_payment`
  and `supplier_payment_void` directly.

## Recording and voiding

- `recordPayment` takes the purchase lock, reads the purchase's ceiling and paid total, and records the payment as a
  one-element `recordPaymentsForLockedPurchases` batch. A delivery-level payment cannot push that delivery's paid total
  above its delivery total. Once the purchase is fully delivered, the purchase's paid total cannot exceed the delivered
  total; before that, the ceiling from `PurchasePaymentCeilingService` decides the resulting status. The service saves
  the row, stores the status from `PurchasePaymentStatusService.resolvePaymentStatus` and publishes the Kafka event
  through `SupplierPaymentHandlerForKafka`.
- `recordPaymentsForLockedPurchases` records a batch for the bulk payment grid. It never takes purchase locks: the
  caller must hold the advisory lock of every purchase in the batch before it reads what it passes in, because the
  overpayment checks and the stored statuses are computed from the ceilings and already-paid totals per purchase, and
  the events and responses from the `PurchaseDataFetcher.PurchaseInfo` per purchase. Both callers do this
  (`recordPayment` and `PurchaseBulkPaymentService.record`, which locks in ascending `id` order). The method allows one
  payment per purchase and requires an open fiscal period for each distinct payment date. It applies the same
  fully-delivered balance (the payable from `PurchasePayableCalculator`) and delivery-level checks as `recordPayment`,
  saves all rows together, stores every purchase status in one `PurchaseUpdater.updatePaymentStatuses` call, then
  publishes each event and builds the responses from the infos it was given with the stored status applied, so it never
  re-reads the purchases and the query count does not grow with the batch.
- `voidPayment` requires an open fiscal period for today, takes the purchase lock, rejects an already-voided payment,
  saves the void row, stores the status resolved from the paid total read before the void minus the voided amount
  (`PurchasePaymentStatusService.patchThenReturnPaymentStatusAfterVoid`) and publishes the void event through
  `SupplierPaymentVoidHandlerForKafka`.
- Ledger postings are produced by the Kafka consumers of the recorded and voided events, not here.
