# Purchase Package — Rules & Expectations

This document is the canonical reference for **how a purchase behaves** in
RTSS. It is intended to be consumed by humans browsing the codebase **and**
by RAG agents that need to answer questions like *"can a fully delivered
purchase be canceled?"* without re-reading every class.

The purchase domain is the supplier-facing twin of `sale/`. It tracks
**what was ordered**, **what has been delivered**, **what has been
canceled**, and **what has been paid** to the supplier. Stock comes IN here
via deliveries, while it goes OUT in the sale package.

If you are about to change behavior inside this package, scan this file
first and update it when the rule changes.

---

## 1. Scope & Public Surface

The purchase domain spans several packages:

```
locations/business/purchase/          — purchase orders & lines
locations/business/delivery/          — deliveries against a purchase
locations/business/supplier_payment/  — payments to the supplier
locations/business/supplier_return/   — returns to the supplier (entity only)
```

The public surface is the `api/` sub-package of each domain.
Cross-package callers **MUST** use these entry points (enforced by
`ArchitectureTest`, which forbids access to non-`api` classes from
outside the domain). `PurchaseAssembler` lives in the parent package
rather than under `api/` only because nothing outside the purchase
domain needs it — internally all three write services and
`PurchaseDataFetcher` route their responses through it.

| Class                           | Purpose                                                          |
|---------------------------------|------------------------------------------------------------------|
| `PurchaseService`               | Create / update draft, create order, convert draft → order       |
| `PurchaseUpdater`               | Update notes (REST-exposed); patch payment status (intra-domain) |
| `PurchaseCanceller`             | Cancel quantities on existing lines                              |
| `PurchaseDataFetcher`           | Read purchases (recent list, lookups, info-by-ids)               |
| `PurchaseAssembler`             | Build `PurchaseResponseDto` from entities                        |
| `DeliveryHandlerForPurchase`    | Prepare a delivery context; commit a delivery to lines           |
| `PurchasePaymentCeilingService` | Compute the payable ceiling for a purchase                       |
| `PurchaseDeliveryService`       | Record a delivery (with Kafka event + payment status patch)      |
| `PurchaseDeliveryDataFetcher`   | Read deliveries by purchase/delivery id                          |
| `SupplierPaymentService`        | Record / void supplier payments                                  |
| `PurchasePaymentStatusService`  | Recompute and persist payment status                             |

Direct use of `PurchaseRepository`, `PurchaseEntity`, etc. is reserved for
code inside the purchase package itself.

---

## 2. Lifecycle / Status State Machine

A purchase lives in one of five states (`PurchaseStatus`):

```
                +---------+   confirm/convert    +---------+
   create ----> |  DRAFT  | -------------------> | ORDERED |
                +---------+                      +---------+
                                                      |
                                                      |  any delivery
                                                      v
                                              +---------------------+
                                              | PARTIALLY_DELIVERED |
                                              +---------------------+
                                                      |
                              +-----------------------+----------------------+
                              |                                              |
                              |  delivered + canceled == ordered             |
                              |  AND some quantity was delivered             |
                              v                                              |
                +-----------------+                                          |
                | FULLY_DELIVERED |◄─────────────────────────────────────────+
                +-----------------+

                +---------+    cancel 100% of every line, no delivery    +----------------+
                | ORDERED | ────────────────────────────────────────────►| CANCELED |
                +---------+                                              +----------------+
                                                                              (terminal)
```

Rules:

- A purchase **starts** at `DRAFT` (via `PurchaseService.createDraft`) or
  jumps straight to `ORDERED` (via `PurchaseService.createOrder`).
- A `DRAFT` can be **mutated** freely via `PurchaseService.updateDraft` —
  supplier, notes, dateOrdered, orderedBy, lines added/updated/deleted.
  `updateDraft` (and only `updateDraft`) is gated by `guardIsDraft`; other
  mutation paths **do** exist for non-draft purchases and are intentional:
  `PurchaseUpdater.updateNotes` on any status,
  `PurchaseCanceller.cancel` on `ORDERED` / `PARTIALLY_DELIVERED`,
  delivery flows that advance status to `PARTIALLY_DELIVERED` /
  `FULLY_DELIVERED`, `SupplierPaymentService.recordPayment` /
  `voidPayment` on non-draft purchases, and
  `PurchaseUpdater.updatePaymentStatus` driven internally by the payment
  service.
- A `DRAFT` becomes `ORDERED` only through
  `PurchaseService.convertDraftToOrder`, which requires ≥1 surviving line.
- Once `ORDERED`, deliveries move the purchase to `PARTIALLY_DELIVERED`
  and then to `FULLY_DELIVERED` when every line's `remainingQuantity ≤ 0`
  (i.e. `quantityDelivered + quantityCanceled ≥ quantityOrdered`). The
  status transition is computed inside
  `DeliveryHandlerForPurchase.commitDelivery`.
- `PurchaseCanceller.cancel` recomputes status differently: if every line
  is fully accounted for **and at least one line had a delivery** →
  `FULLY_DELIVERED`. If every line is fully accounted for but **nothing
  was ever delivered** → `CANCELED`. If at least one line has any
  delivery → `PARTIALLY_DELIVERED`. Otherwise the existing status is
  kept. See `PurchaseCanceller.resolvePurchaseStatus`.
- Only `ORDERED` and `PARTIALLY_DELIVERED` accept deliveries
  (`PurchaseValidator.guardCanReceiveDelivery`). `DRAFT` is rejected
  with a "confirm the order first" message, and `FULLY_DELIVERED` /
  `CANCELED` are both terminal.

`PurchaseStatus` is persisted as a 5-char code via `PurchaseStatusConverter`
(`DFT`, `ORD`, `PDL`, `FDL`, `CLD`). Never `@Enumerated`.

### Delivery status

`PurchaseDeliveryStatus`: `PROCESSING` ("P") → `RECEIVED` ("R") / `FAILED`
("F"). A delivery is created in `PROCESSING` and is flipped to `RECEIVED`
**only** when `PurchaseDeliveryInventoryProcessor.handle` successfully
applies it to stock. `FAILED` is reserved for future error handling and is
not currently set by any flow.

---

## 3. Identity: Supplier, OrderedBy, DateOrdered

### supplierId

- A purchase is always tied to **exactly one supplier** (a contact). There
  is no walk-in equivalent — every purchase identifies a supplier.
- `supplierId` is non-null on the entity. The service layer does not
  currently call `contactService.guardExists` for the supplier — callers
  are trusted to supply a valid id (and the FK would catch garbage at
  insert time).

### orderedById

- Optional on `DRAFT`. Backfilled from `SessionContextProvider.getUserId()`
  on `createOrder` and `convertDraftToOrder` if absent.
- Stored on the purchase; surfaced via `userQualifier.getUserFullName` in
  the response.

### dateOrdered

- Optional on `DRAFT`.
- On `createOrder` and `convertDraftToOrder`, missing `dateOrdered` defaults
  to `DateTimes.Offset.Now.organization()` (org-zoned now).
- Unlike sale's `dateSold`, **no future-date guard** and **no fiscal-period
  guard** is applied at order time. Fiscal-period validation only happens
  on `recordDelivery`, and it operates on a `LocalDate` derived from the
  delivery's `OffsetDateTime` at the org timezone:
  `fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(dto.deliveredAt))`.

---

## 4. Purchase Lines

### Identity & quantities

A `PurchaseLineEntity` belongs to exactly one `PurchaseEntity`. Each line
tracks **three quantities** that together describe its lifecycle:

| Field               | Meaning                                                              |
|---------------------|----------------------------------------------------------------------|
| `quantityOrdered`   | What the supplier was asked to send (in the line's `unitId`).        |
| `quantityDelivered` | Cumulative quantity received across all deliveries (line's unit).    |
| `quantityCanceled`  | Quantity the buyer has canceled (will not be received).              |

Derived values (`PurchaseLineEntity`):

- `getExpectedQuantity() = quantityOrdered − quantityCanceled`
- `getRemainingQuantity() = getExpectedQuantity() − quantityDelivered`
- `getTotalCost() = unitCost × getExpectedQuantity()` (scale 4)

`locationProductId` is `updatable = false` on the entity — once a line is
created the product is fixed. Quantity, cost, and unit can change while
the purchase is in `DRAFT`; only `quantityDelivered` / `quantityCanceled`
change after `ORDERED`.

### Per-line invariants

1. `quantityOrdered` carries the unit chosen by the user (`unitId`).
   `conversionFactor` is the multiplier to the product's base unit and
   is resolved by `locationProductDataFetcher.getConversionFactors`.
2. **`quantityOrdered` must be positive** on create
   (`PurchaseValidator.guardPositiveLineQuantities`, enforced by
   `PurchaseService.createDraft` / `createOrder` and inside
   `PurchaseLinesResolver.computeAdditions` for additions during an
   update). On `linesToUpdate`, the same guard is relaxed to
   "non-negative" (`PurchaseValidator.guardNonNegativeUpdateQuantity`)
   because zero is the delete signal — see Update DTO contract below.
3. **No duplicate products** in the same purchase
   (`PurchaseValidator.guardNoDuplicateProducts`). Checked at create and
   after the merged update set in both `updateDraft` and
   `convertDraftToOrder`.
4. `locationProductId` must be **active**
   (`LocationProductService.guardAllActive`) at create and on every newly
   added line during update.
5. `unitCost` is per the line's `unitId`. Precision `(15, 2)` — purchase
   costs are 2 decimal places, not 4.
6. Canceling on a draft is **not** an `update` operation — there is no
   `quantityCanceled` field on `PurchaseLineUpdateDto`. Use
   `PurchaseCanceller.cancel` after the purchase is `ORDERED`.

### Update DTO contract

`PurchaseUpdateDto` is **always partial**:

- `linesToAdd`: new lines (same shape as create). Product must be
  active. Quantity must be strictly positive.
- `linesToUpdate`: existing line ids with new `quantityOrdered` /
  `unitId` / `unitCost`. There is **no separate `linesToRemove`** field
  — `PurchaseLinesResolver.computeUpdates` treats a `quantityOrdered` of
  exactly zero as a delete signal and routes the line into `toDelete`.
  Negative quantities are rejected
  (`PurchaseValidator.guardNonNegativeUpdateQuantity`).
- A line id that is not in the purchase is silently skipped by
  `existingLinesById[lineDto.id] ?: continue`. The UI passes server-issued
  GUIDs through, so a "typo" isn't a real failure mode — but be aware
  that a stale id (e.g. a line that was deleted by another tab) simply
  no-ops rather than throwing.
- Updates on a non-draft purchase are blocked by `guardIsDraft`.

`PurchaseLinesResolver.detanglePurchaseLines` produces a single
`LineUpdateResult(toDelete, toUpdate, toCreate, resultingLines)`, and
`PurchaseService.persistLineUpdates` deletes-then-saves in one go.

### "Resulting lines"

After an update, the **resulting line set** is the input to:

- the no-duplicates check,
- the "must have ≥1 line" check on order conversion,
- the response built by the assembler.

---

## 5. Deliveries

Deliveries are the only mechanism by which stock enters the system. They
live in the `delivery/` package and are coordinated through
`PurchaseDeliveryService.recordDelivery`.

### Flow

1. `fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(dto.deliveredAt))`
   — gating first; stock movement requires an open period. The DTO's
   `deliveredAt` is an `OffsetDateTime`, converted to a `LocalDate` at
   the organization's timezone before the check.
2. `deliveryHandlerForPurchase.prepareForDelivery(purchaseId)` — calls
   `PurchaseDataFetcher.lockAndGetPurchase` (acquires the purchase
   advisory lock + loads the entity), runs
   `PurchaseValidator.guardCanReceiveDelivery` (only `ORDERED` and
   `PARTIALLY_DELIVERED` are allowed; `DRAFT`, `FULLY_DELIVERED`, and
   `CANCELED` are rejected), and returns a
   `PurchaseDeliveryContext` (purchase id, supplier id, line dtos).
3. `purchaseDeliveryValidator.validate(dto, context.purchaseLineById)`:
   - Delivery must have at least one line.
   - No duplicate purchase line ids in the delivery payload.
   - Each delivery line points to a real purchase line.
   - Quantity > 0; unit cost > 0.
   - Quantity converted to the purchase line's `unitId` must not exceed
     `remainingQuantity` (`quantityOrdered − quantityCanceled − quantityDelivered`).
   - There is **no product-active check** here. Active status is enforced
     only on the purchase-side flows (create / convertDraftToOrder /
     update lines). Once a purchase has been ordered, the supplier is on
     the hook to deliver and we accept their shipment even if the
     product is later deactivated locally.
4. Persist a `PurchaseDeliveryEntity` (status `PROCESSING`) + its
   `PurchaseDeliveryLineEntity` rows.
5. `commitDelivery` adds `quantityDelivered` to each purchase line (in
   the line's own unit, after a unit conversion) and updates the purchase
   status to `PARTIALLY_DELIVERED` / `FULLY_DELIVERED`.
6. Publish `PurchaseDeliveredEvent`. The synchronous transaction returns
   the response **before** stock is updated.
7. `PurchaseDeliveryInventoryProcessor.handle` (async, separate
   transaction):
   - Records stock movements via `PurchaseDeliveryStockUpdater`.
   - Updates `LocationProduct.lastPurchasePrice` from
     `event.lines.unitCost`.
   - Flips the delivery from `PROCESSING` → `RECEIVED`.
8. Payment status is recomputed at the end of `recordDelivery`
   (`purchasePaymentStatusService.patchThenReturnPaymentStatus`).

### Delivery line ↔ purchase line unit conversion

Suppliers may deliver in a unit different from the purchase line's
`unitId`. `PurchaseDeliveryValidator` calls
`unitConversionGraphFacade.getFactor(lineDto.unitId, purchaseLine.unitId)`
to convert and check against `remainingQuantity`. The delivery line
itself stores `quantityDelivered` in its **own** `unitId`. The
**purchase line** accumulates `quantityDelivered` in the **purchase
line's** `unitId` (after conversion). Be mindful of this when reasoning
about stock totals.

### Idempotency

- `PurchaseDeliveryInventoryProcessor.shouldProcess` checks the delivery
  is not already `RECEIVED`.
- The DB constraint `add-unique-constraint-stock-movement-ext-ref-type-product`
  ensures the stock-movement table cannot record the same delivery twice.
- Combined, retries are safe: a retried event either skips (already
  RECEIVED) or no-ops at the stock layer (constraint blocks the duplicate
  row), and the delivery row is then flipped.

---

## 6. Cancellation

Two distinct flavors of "cancel" exist; do not confuse them.

### Line cancellation (`PurchaseCanceller.cancel`)

- Allowed only on `ORDERED` or `PARTIALLY_DELIVERED` purchases
  (`guardCanCancelLines`).
- **Cancellation is absolute, not incremental.** Each
  `PurchaseCancelLinesDto(purchaseLineId, quantityCanceled)` *sets* the
  line's `quantityCanceled` to that exact value — there is no "cancel an
  additional N" operation. Callers should always send the cumulative
  total they want recorded.
- `guardCancelQuantity` rejects `quantityCanceled > (quantityOrdered − quantityDelivered)`
  — already-delivered units cannot be canceled. The DTO value replaces the prior
  `quantityCanceled` outright, so reducing a prior cancellation is also legal — pass
  a smaller number.
- After applying cancellations, the purchase status is recomputed by
  `resolvePurchaseStatus`:
  - All lines accounted for (`delivered + canceled == ordered`) **and at
    least one line had a delivery** → `FULLY_DELIVERED`.
  - All lines accounted for **but nothing was ever delivered** →
    `CANCELED`.
  - Otherwise, if any line has any delivery → `PARTIALLY_DELIVERED`.
  - Otherwise the existing status is kept.
- Payment status is recomputed at the end
  (`patchThenReturnPaymentStatus`).

### Purchase-level `CANCELED`

- A purchase reaches `CANCELED` only via `PurchaseCanceller.cancel`,
  when the caller cancels every uncanceled unit on every line **and no
  delivery has ever been recorded** against the purchase.
- It is **terminal**: `guardCanReceiveDelivery` rejects it, and there
  is no flow that resurrects a `CANCELED` purchase. If a buyer
  changes their mind, the right action is to create a new purchase.
- Do not confuse it with line-level cancellation, which is the mutation
  (the `quantityCanceled` column). `CANCELED` is the *outcome* of
  one specific cancellation pattern; line cancellation is the
  *operation*.

---

## 7. Payments to the Supplier

Payments live in the `supplier_payment` package. Unlike sale payments,
they are **never bundled with the purchase create/update payload** — they
are always recorded post-hoc against an existing purchase.

### Ceiling — *what is the most we should pay this supplier?*

`PurchasePaymentCeilingService.computeCeiling(purchaseId)` answers that
question for a given purchase. It compares two views of value:

| Number           | Formula                                                         | Source                                      |
|------------------|-----------------------------------------------------------------|---------------------------------------------|
| `poTotal`        | Σ over lines: `unitCost × (quantityOrdered − quantityCanceled)` | The agreed PO price for what still stands.  |
| `deliveredTotal` | Σ over delivery lines: `unitCost × quantityDelivered`           | The actual cost of what has shipped so far. |

The two can differ in two ways:

1. **Different quantities.** A supplier may under- or over-deliver
   against the order.
2. **Different unit costs.** A delivery line records its **own**
   `unitCost`, which may not match the purchase line's price.

The ceiling reconciles those views by stage:

| Stage                                        | Ceiling                        | Why                                                                                                                                              |
|----------------------------------------------|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Nothing delivered yet                        | `poTotal`                      | We've only agreed to the PO — pay no more than what we ordered.                                                                                  |
| Some delivered, more still expected          | `max(poTotal, deliveredTotal)` | Mid-flight: if the supplier over-delivered/over-charged, we owe the higher number; if they undercharged, we still owe the PO. Protect both ways. |
| Fully delivered (every line's remaining ≤ 0) | `deliveredTotal`               | The PO is now historical; what we *actually received* is what we owe.                                                                            |

"Fully delivered" requires `deliveredTotal > 0` AND every line's
`remainingQuantity ≤ 0` — i.e. `quantityDelivered + quantityCanceled ≥
quantityOrdered` for every line. Once true, the ceiling **locks** to
`deliveredTotal` and the `recordPayment` guard kicks in (see below).

### Record payment (`SupplierPaymentService.recordPayment`)

1. Amount must be `> 0`.
2. **Delivery-level guard** (only when `dto.deliveryId != null`): the
   sum of payments tagged with that delivery cannot exceed that
   delivery's single-delivery total.
3. **Fully-delivered guard**: when the purchase is fully delivered, the
   running total paid cannot exceed `deliveredTotal`.
4. ⚠ **There is no purchase-level ceiling guard for a purchase that is
   NOT yet fully delivered and where `deliveryId` is null.** A caller
   can over-pay a partially-delivered or ORDERED purchase without
   tripping any throw — the resulting `paymentStatus` will just resolve
   to `OVERPAID`. The `paymentCeiling` returned in `PurchaseResponseDto`
   is informational; only the two cases above are *enforced* server-side.
5. Persist `SupplierPaymentEntity`.
6. Recompute `paymentStatus`:
   - `paid == 0` → `UNPAID`
   - `paid > ceiling` → `OVERPAID`
   - `paid == ceiling` → `FULLY_SETTLED`
   - else → `PARTIALLY_SETTLED`
7. Publish `SupplierPaymentEvent` **only when the payment method has an
   account code** (cash-only methods skip the ledger event).

### Void payment

- Cannot void a payment that is already voided
  (`existsBySupplierPaymentId`).
- Records a `SupplierPaymentVoidEntity`, recomputes the purchase's
  payment status, and emits a void event (again, only if the payment
  method has an account code).

### Status helpers

`PurchasePaymentStatusService.patchThenReturnPaymentStatus(purchaseId)` is
the **single source of truth** for recomputing a purchase's payment
status. Call it after any flow that changes lines, deliveries, or
payments. `recordDelivery`, `PurchaseCanceller.cancel`, and
`voidPayment` all do.

⚠ **`recordPayment` is the one deliberate exception.** It already
fetched the ceiling and `alreadyPaid` for the over-payment guard, so
it inlines `resolvePaymentStatus(alreadyPaid + amount, ceiling)` and
calls `PurchaseUpdater.updatePaymentStatus` directly instead of
re-fetching everything in `patchThenReturnPaymentStatus`. If you add a
new payment-related flow, prefer `patchThenReturnPaymentStatus` unless
you have the same "already fetched, don't re-query" justification.

`PaymentStatus` is shared with the sale domain (same converter on
`PurchaseEntity` and `SaleEntity`) and is persisted as a 5-char code:
`UNPAID` (UNP), `PARTIALLY_SETTLED` (PST), `FULLY_SETTLED` (FST),
`OVERPAID` (OVP).

---

## 8. Persistence & Transactions

- All entry points use `@TransactionalOnLocationSchema` so the schema
  search path is per-location. Read paths (`PurchaseDataFetcher` at the
  class level, `PurchaseDeliveryDataFetcher`, `PurchasePaymentCeilingService`,
  the Kafka handlers' `reissue`, `DeliveryHandlerForPurchase.getDeliveryContext`)
  declare `readOnly = true`. `prepareForDelivery` and `commitDelivery`
  are explicitly **writable** — `prepareForDelivery` acquires the
  advisory lock, `commitDelivery` mutates line totals and purchase
  status.
- `recordDelivery` runs **delivery rows + purchase-line accumulation +
  purchase status** in one transaction. Stock is updated in a **separate
  transaction** by `PurchaseDeliveryInventoryProcessor` after the
  `PurchaseDeliveredEvent` lands.
- Kafka events use `ApplicationEventPublisher` and bind to commit — a
  rolled-back transaction never publishes.
- Audit trail: `PurchaseEntity`, `PurchaseLineEntity`,
  `PurchaseDeliveryEntity`, `PurchaseDeliveryLineEntity`, and
  `SupplierReturnEntity` are `@Audited`. `SupplierPaymentEntity` and
  `SupplierPaymentVoidEntity` are not (immutable rows).

---

## 9. Locking

Every flow that mutates anything keyed to a purchase (header, lines,
deliveries, payments) goes through `PurchaseDataFetcher`, which exposes
two helpers over `EntityAdvisoryLock.acquire(LockNamespaces.PURCHASE, ...)`:

- `lockAndGetPurchase(purchaseId)` — lock + `getReferenceById` for
  flows that immediately mutate the `PurchaseEntity`. Used by
  `PurchaseService.updateDraft`, `PurchaseService.convertDraftToOrder`,
  `PurchaseCanceller.cancel`, and
  `DeliveryHandlerForPurchase.prepareForDelivery` (invoked by
  `PurchaseDeliveryService.recordDelivery`).
- `lockPurchase(purchaseId)` — lock only, no entity load. Used by
  `SupplierPaymentService.recordPayment` / `voidPayment`, which need
  the serialization but mutate `SupplierPaymentEntity` /
  `SupplierPaymentVoidEntity` rather than the purchase header
  directly (`paymentStatus` updates funnel through
  `PurchaseUpdater.updatePaymentStatus`).

Concurrent operations on the same purchase therefore serialize at the
application layer instead of racing on `quantityDelivered` /
`quantityCanceled` / `purchaseStatus` / `paymentStatus` or on the
ceiling-vs-paid-total read used by the payment guards. Don't acquire
the lock manually elsewhere — go through one of these helpers.

Both helpers declare `Propagation.MANDATORY` so they can only run
inside an existing writable transaction. That keeps the lock bound to
the same transaction as the subsequent mutations.

The single-field setters `PurchaseUpdater.updateNotes` and
`PurchaseUpdater.updatePaymentStatus` do **not** acquire the lock
themselves. `updatePaymentStatus` is always invoked from inside one
of the flows above (delivery, cancel, recordPayment, voidPayment), all
of which acquire the lock first. `updateNotes` writes only the notes
column — low contention risk; we accept the lack of locking there.

The DB unique constraint
`add-unique-constraint-stock-movement-ext-ref-type-product` provides a
second layer of safety inside the async `PurchaseDeliveryInventoryProcessor`
— it guarantees retries can never double-post the same delivery to
stock.

---

## 10. Mapping & DTO Conventions

These rules mirror the project-wide convention in `CLAUDE.md`/`instructions.md`,
called out here because the purchase package is a common reference point.

### Entities vs DTOs

- Entities (`PurchaseEntity`, `PurchaseLineEntity`, `PurchaseDeliveryEntity`,
  `PurchaseDeliveryLineEntity`, `SupplierPaymentEntity`) carry only column
  annotations and insert/update restrictions. Tiny helpers live on the
  *line* entities: `PurchaseLineEntity.getExpectedQuantity` /
  `getRemainingQuantity` / `getTotalCost` and
  `PurchaseDeliveryLineEntity.lineTotal`. The header entities have no
  helper methods.
- Domain DTOs (`PurchaseLineDto`, `PurchaseDeliveryContext`,
  `PurchasePaymentCeilingService.PaymentCeiling`,
  `PurchaseDataFetcher.PurchaseInfo`) abstract DB details and are what
  cross package boundaries.
- API DTOs (`PurchaseCreateDto`, `PurchaseUpdateDto`,
  `PurchaseLine*Dto`, `PurchaseCancelLinesDto`,
  `PurchaseDeliveryCreateDto`, `SupplierPaymentCreateDto`) are request
  shapes.

### Insert flow

`PurchaseMapper.toLineEntities` mirrors the insert flow described in
`CLAUDE.md` — DTO → entity. Generated fields come from
`HasReferenceEntity` / `ImmutableEntity`.

### Update flow

`PurchaseMapper.applyDraftUpdate` uses two patterns depending on the
field:

- `notes`, `dateOrdered`, `orderedById` are `Optional<T>?` — the standard
  three-state convention:
  - `null` = "not provided, leave it alone"
  - `Optional.empty()` = "explicitly clear it"
  - `Optional.of(x)` = "set to x"
- `supplierId` is a plain `UUID?` — only two states: `null` = skip,
  `non-null` = set. Clearing is not legal (the column is non-null), so
  the three-state form would be misleading here.

`convertDraftToOrder` differs again: `dateOrdered` and `orderedById`
are **unconditionally rewritten** on the entity using
`dto.dateOrdered?.orElse(null) ?: DateTimes.Offset.Now.organization()`
(same shape for `orderedById`). The DTO is the source of truth at
convert time — if the user had set `dateOrdered` while the purchase was
in `DRAFT` and then converts without resending it, the previously-stored
draft value is **clobbered** with NOW. This is intentional: convert is
the moment the order is actually placed.

### Reference numbers

Generated by DB sequences via `HasReferenceEntity` (prefix declared in
the table registry). Surfaced via `requiredReference()` once persisted.
Kafka publishers depend on this — never publish an event with a null
reference.

### Serializable

DTOs in `purchase/api/` are `Serializable` (used by remote/RPC layers and
some cache paths). DTOs in `delivery/api/` and `supplier_payment/api/` are
**not**, and neither are sale API DTOs. Match the existing convention of
whichever `api/` package you are adding to.

---

## 11. Kafka & Idempotency

Events produced by this domain:

| Event                          | Trigger                                                          | Handler(s)                           |
|--------------------------------|------------------------------------------------------------------|--------------------------------------|
| `PurchaseDeliveredEvent`       | `PurchaseDeliveryService.recordDelivery`                         | `PurchaseDeliveryInventoryProcessor` |
| `SupplierPaymentRecordedEvent` | `SupplierPaymentService.recordPayment` (if account code present) | accounting/ledger                    |
| `SupplierPaymentVoidedEvent`   | `SupplierPaymentService.voidPayment` (if account code present)   | accounting/ledger                    |

Rules every new processor must follow:

1. Implement `shouldProcess` as a fast existence check on the natural key
   of whatever the processor writes.
2. Back that check with a **unique DB constraint** on the natural key.
   Example: `add-unique-constraint-stock-movement-ext-ref-type-product`
   blocks duplicate stock movements per (delivery, product).
3. Be re-entrant: a retried event must produce the same end state.
4. Wrap `handle` in `@TransactionalOnLocationSchema`; `shouldProcess` may
   be `readOnly = true`.

Republish path: every Kafka publisher in this domain implements
`EventReissueHandler` so administrative replays
(`DeliveryHandlerForKafka.reissue`, supplier payment handlers' reissue)
rebuild the same payload from the database.

---

## 12. Validation Ordering

When extending these flows, preserve the order:

1. **Pure DTO guards first** (free): no duplicates, has lines, positive
   amounts, etc.
2. **Cheap status guards next** (single FK by id): `guardIsDraft`,
   `guardCanCancelLines`, `prepareForDelivery`.
3. **Single-row lookups** (purchase, supplier).
4. **Bulk fetches** (line list, product summaries, unit conversion
   graph, deliveries) — these are the expensive ones and should run only
   after the above pass.
5. **Cross-line invariants** (remaining quantity, ceiling).
6. **Persist** (entities, status, payment status).
7. **Publish Kafka events** (last, so the transaction is shaped
   correctly).

Add new guards as early as the data they need is in scope.

---

## 13. Common Pitfalls

- **Stock is updated asynchronously.** `recordDelivery` returns success
  before the inventory processor has run. Code that needs stock to
  reflect the delivery cannot rely on synchronous read-back.
- **No product-active guard on delivery.** Active status is checked on
  the purchase-creation side; once ordered, deliveries against an
  inactivated product are still accepted by design (see §5).
- **`PurchaseUpdateDto` has no `linesToRemove`.** Send
  `quantityOrdered = 0` in `linesToUpdate` to delete a line. This is a
  deliberate departure from the sale package.
- **No fiscal-period guard on order creation.** Only deliveries enforce
  fiscal periods. If you need order-time fiscal enforcement, add it
  consciously — there's no precedent in this package.
- **Forgetting to recompute payment status.** Any flow that changes
  lines, deliveries, or payments must end with
  `purchasePaymentStatusService.patchThenReturnPaymentStatus`. Delivery
  and cancel paths already do; new entry points must too.
- **Mixing PO totals and delivered totals.** Reads that need "what does
  this purchase cost" should always go through
  `PurchasePaymentCeilingService.computeCeiling` rather than summing
  lines or deliveries directly — the rules are non-obvious (see §7).

---

## 14. Quick Reference: Each Public Entry Point

### `PurchaseService.createDraft(PurchaseCreateDto)`
- Rejects duplicate products.
- Requires all products to be active.
- Does **not** enforce "at least one line" — drafts may start with an
  empty `linesToAdd`. When `linesToAdd` is non-empty, every line must
  have a strictly positive `quantityOrdered` (`guardPositiveLineQuantities`
  iterates with `any{}`, so an empty list passes vacuously).
- Creates `PurchaseEntity` at `DRAFT`, lines with resolved conversion
  factors.

### `PurchaseService.updateDraft(PurchaseUpdateDto)`
- Loads the purchase via `PurchaseDataFetcher.lockAndGetPurchase` (lock
  + entity in one call).
- Purchase must be `DRAFT`.
- Applies header edits via `applyDraftUpdate`.
- Detangles line changes via `PurchaseLinesResolver` (zero-qty rows are
  deletes).
- Rejects duplicate products on the resulting set.

### `PurchaseService.createOrder(PurchaseCreateDto)`
- Requires ≥1 line, all products active, no duplicates, every quantity
  positive.
- Creates `PurchaseEntity` at `ORDERED`, defaulting `dateOrdered` to
  `DateTimes.Offset.Now.organization()` and `orderedById` to
  `SessionContextProvider.getUserId()` when the DTO omits them.

### `PurchaseService.convertDraftToOrder(PurchaseUpdateDto)`
- Loads the purchase via `PurchaseDataFetcher.lockAndGetPurchase`.
- Purchase must be `DRAFT`.
- Applies header + line changes; requires ≥1 surviving line.
- Backfills `dateOrdered` / `orderedById` if missing.
- Flips status to `ORDERED`.

### `PurchaseCanceller.cancel(purchaseId, lines)`
- Loads the purchase via `PurchaseDataFetcher.lockAndGetPurchase`.
- Purchase must be `ORDERED` or `PARTIALLY_DELIVERED`.
- Sets `quantityCanceled` on each named line (overwrites).
- Recomputes purchase status — can land at `FULLY_DELIVERED`,
  `CANCELED`, `PARTIALLY_DELIVERED`, or leave it unchanged. See
  §6 for the resolution rules.
- Recomputes payment status.

### `PurchaseDeliveryService.recordDelivery(PurchaseDeliveryCreateDto)`
- Requires open fiscal period for `deliveredAt`.
- `prepareForDelivery` loads via `PurchaseDataFetcher.lockAndGetPurchase`
  (lock taken inside that call).
- Purchase status must be `ORDERED` or `PARTIALLY_DELIVERED`
  (`PurchaseValidator.guardCanReceiveDelivery` — exhaustive `when`, so
  any new status forces an update).
- Each line: positive qty + unit cost; converted qty fits in remaining.
  No product-active check — see §5.
- Persists delivery + lines (status `PROCESSING`), commits to purchase
  lines, publishes `PurchaseDeliveredEvent`, recomputes payment status.
- Stock and `lastPurchasePrice` update asynchronously; delivery row
  flips to `RECEIVED` when the processor succeeds.

### `SupplierPaymentService.recordPayment(SupplierPaymentCreateDto)`
- Locks the purchase via `PurchaseDataFetcher.lockPurchase`.
- Amount must be `> 0`.
- Delivery-level guard when `deliveryId` is set (sum of payments tagged
  with that delivery ≤ that delivery's `lineTotal` sum).
- Fully-delivered guard once `ceiling.isFullyDelivered` (running total
  paid + this amount ≤ `deliveredTotal`).
- Persists `SupplierPaymentEntity`, then **inlines** the status update:
  calls `purchasePaymentStatusService.resolvePaymentStatus(alreadyPaid +
  amount, ceiling)` and `purchaseUpdater.updatePaymentStatus` directly,
  reusing the already-fetched ceiling and total. It does NOT call
  `patchThenReturnPaymentStatus` — see §7 for why this is the one
  exception to that rule.
- Kafka event emitted only if the payment method has an account code.

### `SupplierPaymentService.voidPayment(SupplierPaymentVoidCreateDto)`
- Locks the purchase via `PurchaseDataFetcher.lockPurchase` (resolved
  from the payment's `purchaseId`).
- Rejects an already-voided payment via
  `supplierPaymentVoidRepository.existsBySupplierPaymentId`.
- Persists `SupplierPaymentVoidEntity` and recomputes the purchase
  payment status via `patchThenReturnPaymentStatus`.
- Kafka void event emitted only if the payment method has an account code.

### `PurchaseUpdater.updateNotes(id, Optional<String>?)` / `updatePaymentStatus(id, status)`
- Direct field updates by id; no business validation.
- `updateNotes` is REST-exposed via `PurchaseEndpoint`. `updatePaymentStatus`
  is intra-domain — only `PurchasePaymentStatusService` and
  `SupplierPaymentService.recordPayment` call it, and both arrive with the
  purchase lock already held.
- `updateNotes` uses the same `Optional<String>?` partial-update
  convention as `PurchaseUpdateDto`: outer `null` = no-op, empty
  Optional = clear, present Optional = set. The empty/present case is
  funneled through `StringUtils.getValueOrNull` so blank strings
  collapse to null.

### `PurchaseDataFetcher.fetchTop(n?)`
- Defaults to 10; rejects `n > 1000`. Sorted by `createdOn DESC`.
- Note: unlike sale's fetchRecent, **no guard against `n ≤ 0`** — a
  negative limit will reach the page request as-is.

---

## 15. Where Things Live

| Topic                          | Class(es)                                                                                         |
|--------------------------------|---------------------------------------------------------------------------------------------------|
| State, header                  | `PurchaseEntity`, `PurchaseStatus`, `PurchaseStatusConverter`                                     |
| Lines                          | `PurchaseLineEntity`, `PurchaseLinesResolver`, `PurchaseMapper`                                   |
| Validation                     | `PurchaseValidator`, `PurchaseDeliveryValidator`                                                  |
| Cancellation                   | `PurchaseCanceller`                                                                               |
| Deliveries                     | `delivery/` package (`PurchaseDeliveryService`, `DeliveryHandlerFor*`)                            |
| Stock (async)                  | `PurchaseDeliveryInventoryProcessor`, `PurchaseDeliveryStockUpdater`                              |
| Payments                       | `supplier_payment/` package                                                                       |
| Payment status (single source) | `PurchasePaymentStatusService`                                                                    |
| Payment ceiling                | `PurchasePaymentCeilingService`                                                                   |
| Returns (scaffolding, no svc)  | `supplier_return/SupplierReturnEntity`; repo at `purchase/SupplierReturnRepository`               |
| Kafka publish/republish        | `DeliveryHandlerForKafka`, `SupplierPaymentHandlerForKafka`, `SupplierPaymentVoidHandlerForKafka` |
| Read APIs                      | `PurchaseDataFetcher`, `PurchaseAssembler`, `PurchaseDeliveryDataFetcher`                         |

Keep this table accurate as the package evolves — it is the entry point
for anyone (or any agent) doing a first-pass investigation.
