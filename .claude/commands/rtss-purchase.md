# RTSS — Purchase Domain

Read this before touching anything in `purchase/`, `delivery/`, `supplier_payment/`, or `supplier_return/`.
Then read `purchase/README.md` before editing and update it after.

---

## Package boundaries

Public surface is the `api/` sub-package of each domain. `ArchitectureTest` enforces this.

| Class                           | Purpose                                                               |
|---------------------------------|-----------------------------------------------------------------------|
| `PurchaseService`               | Create/update draft, create order, convert draft → order              |
| `PurchaseUpdater`               | Update notes (REST-exposed); patch payment status (intra-domain only) |
| `PurchaseCanceller`             | Cancel quantities on existing lines                                   |
| `PurchaseDataFetcher`           | All reads + locking helpers                                           |
| `DeliveryHandlerForPurchase`    | Prepare delivery context; commit delivery to lines                    |
| `PurchaseDeliveryService`       | Record a delivery (Kafka + payment status patch)                      |
| `SupplierPaymentService`        | Record / void supplier payments                                       |
| `PurchasePaymentCeilingService` | Compute payable ceiling                                               |
| `PurchasePaymentStatusService`  | Single source of truth for recomputing payment status                 |

## Purchase lifecycle

```
create → DRAFT → ORDERED → PARTIALLY_DELIVERED → FULLY_DELIVERED
                    ↓ (cancel 100%, no delivery)
                CANCELED  (terminal)
```

- `PurchaseStatus` persisted as short code (`DFT`, `ORD`, `PDL`, `FDL`, `CLD`). Never `@Enumerated`.
- Only `ORDERED` and `PARTIALLY_DELIVERED` accept deliveries.
- Delivery status: `PROCESSING` → `RECEIVED` (flipped by async Kafka processor).

## Locking — always go through these helpers

- `PurchaseDataFetcher.lockAndGetPurchase(id)` — lock + entity load. Used by update, convert, cancel, delivery prep.
- `PurchaseDataFetcher.lockPurchase(id)` — lock only. Used by supplier payment flows.
- Both are `Propagation.MANDATORY` — must join an existing writable transaction.
- Never acquire the advisory lock manually elsewhere.

## Key invariants to preserve

- **`quantityOrdered = 0` in `linesToUpdate` is the delete signal** — there is no `linesToRemove` field.
- **Cancellation is absolute, not incremental.** `quantityCanceled` is set to the exact value passed — it overwrites the prior value.
- **No product-active check on delivery.** Active status is enforced at purchase creation only; once ordered, deliveries against an inactivated product are accepted.
- **No fiscal-period guard on order creation.** Fiscal period is only enforced on `recordDelivery` (keyed to `deliveredAt` at org timezone).
- **Stock is updated asynchronously.** `recordDelivery` returns before the inventory processor runs.
- **`dateSold` equivalent** (`dateOrdered`) has no future-date guard and no fiscal-period guard.
- **Payment status recomputation**: always end with `purchasePaymentStatusService.patchThenReturnPaymentStatus` after any flow that changes lines, deliveries, or payments. The one exception is `recordPayment` — it inlines the status update directly (see README §7).

## Payment ceiling logic

| Stage | Ceiling |
|---|---|
| Nothing delivered | `poTotal` |
| Some delivered, more expected | `max(poTotal, deliveredTotal)` |
| Fully delivered | `deliveredTotal` |

Always use `PurchasePaymentCeilingService.computeCeiling` — never sum lines or deliveries directly.

## Kafka idempotency

- `PurchaseDeliveryInventoryProcessor.shouldProcess` checks delivery not already `RECEIVED`.
- DB constraint `add-unique-constraint-stock-movement-ext-ref-type-product` blocks duplicate stock movements.
- Supplier payment events only emitted when the payment method has an account code.

## Validation ordering (preserve when adding guards)

1. Pure DTO guards (no duplicates, has lines, positive amounts)
2. Cheap status guards (`guardIsDraft`, `guardCanCancelLines`, `prepareForDelivery`)
3. Single-row lookups (purchase, supplier)
4. Bulk fetches (lines, product summaries, unit graph, deliveries)
5. Cross-line invariants (remaining quantity, ceiling)
6. Persist
7. Publish Kafka events (last)

## Pitfalls (from README §13)

- Forgetting `patchThenReturnPaymentStatus` after any line/delivery/payment change.
- Mixing PO totals and delivered totals — go through `computeCeiling`.
- Adding a fiscal-period guard at order time — this is an intentional omission.
- `PurchaseDataFetcher.fetchTop` has no `n ≤ 0` guard (unlike `SaleDataFetcher.fetchRecent`).
