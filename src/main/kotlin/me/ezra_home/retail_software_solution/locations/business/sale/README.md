# Sale Package — Rules & Expectations

This document is the canonical reference for **how a sale behaves** in RTSS.
It is intended to be consumed by humans browsing the codebase **and** by RAG
agents that need to answer questions like *"can a draft be reassigned to a
walk‑in customer?"* without re‑reading every class.

If you are about to change behavior inside this package, scan this file first
and update it when the rule changes.

---

## 1. Scope & Public Surface

The sale package lives at:

```
locations/business/sale/
locations/business/sale/api/
```

Only the `api/` sub-package is considered the public surface. Other packages
must call into sale through one of these classes:

| Class                           | Purpose                                                      |
|---------------------------------|--------------------------------------------------------------|
| `SaleDraftHandler`              | Create / update draft sales                                  |
| `SaleConfirmationHandler`       | Create a sale directly (confirmed), or convert a draft       |
| `SaleUpdater`                   | Void a sale; update payment status; update notes             |
| `SaleDataFetcher`               | Read sales (recent list, context lookup, contact lookup)     |

Sellable product lookup (product + per‑product FIFO stock batch previews
for the sale entry screen) lives on the **product** package, not here —
see `location_product/api/LocationProductForSaleSearchService` and the
`POST /secured/location-products/search-for-sale` endpoint.

`SaleAssembler` builds `SaleResponseDto` from entities but lives at
`sale/SaleAssembler.kt`, not `sale/api/` — it is an internal helper used by
the four handlers above and is **not** a cross-package surface.

Cross‑package callers **MUST** use the `api/` package; direct use of
`SaleRepository`, `SaleEntity`, etc. is reserved for code inside the sale
package itself (enforced by `ArchitectureTest`).

---

## 2. Lifecycle / Status State Machine

A sale lives in one of four states (`SaleStatus`):

```
                +---------+   confirm/convert    +-----------+   void    +--------+
   create ----> |  DRAFT  | -------------------> | CONFIRMED | --------> | VOIDED |
                +---------+                      +-----------+           +--------+
                     |
                     | void on draft
                     v
                +-----------+
                | DISCARDED |   (terminal — never restored)
                +-----------+
```

Rules:

- A sale **starts** at `DRAFT` (via `SaleDraftHandler.createDraft`) or jumps
  straight to `CONFIRMED` (via `SaleConfirmationHandler.createSale`).
- A `DRAFT` can be **mutated** via `SaleDraftHandler.updateDraft` — header,
  lines, discounts, payments. `updateDraft` itself is the only entry point
  that touches lines/discounts on a sale and it is `DRAFT`-gated. Other
  mutation paths **do** exist for non-draft sales and are intentional:
  `SaleUpdater.updateNotes` on any status, `voidSale` on `DRAFT` or
  `CONFIRMED`, `SalePaymentService.recordPayment` / `voidPayment` on
  `CONFIRMED` (with their own status guards), `updatePaymentStatus` driven
  internally by the payment service, and the async
  `SaleTaxFinalizationProcessor` / `SaleTaxReversalProcessor` which write
  `taxTotal` / `grandTotal` after the fact.
- A `DRAFT` becomes `CONFIRMED` only through
  `SaleConfirmationHandler.convertDraftToSale`.
- Voiding a `DRAFT` transitions it to `DISCARDED` (reservations are released,
  no stock movement, no Kafka void event — the sale never affected stock).
- Voiding a `CONFIRMED` sale transitions it to `VOIDED` (stock is restored,
  tax entries reversed via Kafka, `SaleVoidEntity` recorded).
- `VOIDED` and `DISCARDED` are terminal. `guardCanVoid` rejects both, and
  also rejects voiding when there are active (non-voided) payments.

`SaleStatus` is persisted as a 5-char code via `SaleStatusConverter`
(`DFT`, `CFM`, `VD`, `DSC`). Never `@Enumerated`.

---

## 3. Identity: Contact, SoldBy, DateSold

### contactId

- A sale is always tied to **exactly one contact**. Walk-in sales use
  `SystemContact.WALK_IN.id`.
- `SaleCreateDto.resolveContactId()` falls back to walk-in when the incoming
  `contactId` is null or `WALK_IN.id` — this is the intended design for the
  create-sale path. `walkInCustomer()` is computed off `resolveContactId()`,
  so both forms (`null` and explicit `WALK_IN.id`) are treated identically.
- **Drafts cannot be walk-in.** `SaleDraftHandler.createDraft` rejects every
  walk-in payload via `walkInCustomer()` — including the null-contact case.
  `SaleValidator.guardNotReassigningToWalkIn` blocks updates that *reassign*
  an existing draft to walk-in.
- A confirmed walk-in sale **must** be fully paid at confirmation time
  (`SalePaymentService.guardFullPaymentCoverage`).
- For any non-walk-in sale, `ContactService.guardExists` must succeed before
  persistence. The check is skipped entirely for walk-in (no DB lookup),
  which is why `SystemContact.WALK_IN.id` is not required to exist as a
  regular contact row.

### soldById

- Optional on `DRAFT`. On confirmation (or direct create) it is auto-filled
  from `SessionContextProvider.getUserId()` if absent.
- Stored on the sale; surfaced via `userQualifier.getUserFullName` in
  responses.

### dateSold

- Optional on `DRAFT`.
- On confirmation / direct create, missing `dateSold` defaults to
  `DateTimes.Offset.Now.organization()`.
- A non-null `dateSold` is **guarded against the future** at the org's local
  date (`SaleValidator.guardDateSoldIsNotFuture`) and **must** fall inside a
  fiscal period that is open (`FiscalPeriodService.requireOpenForDate`).
- On `DRAFT` updates, these two guards still fire **when payments are
  included** in the payload — drafts without payments may carry a null or
  out-of-period `dateSold`, but the moment money moves, the fiscal window
  must be valid.
- **All date comparisons use the organization's timezone**, not UTC. Always
  funnel through `DateTimes.Local.atOrganizationZone` /
  `DateTimes.Local.Now.organization`. Never `LocalDate.now()` /
  `ZoneOffset.UTC`.

---

## 4. Sale Lines

### Identity

- A `SaleLineEntity` belongs to exactly one `SaleEntity` (`sale_id`, FK).
- A sale line carries: `locationProductId`, `quantity`, `unitId`,
  `conversionFactor` to the product's base unit, and a frozen `unitPrice`.
- **`sale_line.quantity` is stored in the line's `unitId`**, not the base
  unit. `conversionFactor` is stored alongside; `baseQty()` is a derived
  helper (`quantity * conversionFactor`). What gets *persisted* as base qty
  is `sale_line_stock_reservation.quantity_reserved` and the FIFO stock
  requests — never the sale line itself.
- `unitPrice` is **captured from the product at line creation** and is
  immutable thereafter — a price change on the product does not retro-affect
  existing lines.
- `lineTotal() = quantity * unitPrice` (scale 4, half-up via `Decimals`).
- `baseQty() = quantity * conversionFactor` (scale 4).

### Per-line invariants

1. `quantity` must be **strictly positive** at create
   (`guardPositiveLineQuantities`) and at update
   (`SaleLinesUpdatePreparer.applyUpdates` rejects `<= 0` and instructs
   callers to use `linesToRemove` instead).
2. **No duplicate products** in the same sale — checked at create
   (`guardNoDuplicateProducts(productIds)`) and after the merged update set
   (`SaleLinesUpdatePreparer.prepareForUpdate`).
3. `locationProductId` must be **active**
   (`LocationProductService.guardAllActive`) at create and on every newly
   added line during update.
4. `unitId` must be a valid unit reachable in the `UnitConversionGraph` to
   the product's base unit; otherwise `getFactor`/`getTarget` throws.
5. `unitPrice` must be set on the product summary; lines whose product has
   no `unitPrice` are rejected with `"Product X has no unit price"`. The
   create path raises this from `SaleMutator.attachUnitPrices`; the update
   path raises it from `SaleLinesUpdatePreparer.buildAdditions`.
   `SaleLineMapper.toLineEntities` uses `!!` and would only fire if those
   earlier guards were bypassed.

### Update DTO contract

`SaleUpdateDto` is **always partial**:

- `linesToAdd`: new lines (same shape as create).
- `linesToUpdate`: existing line ids with new `quantity` / `unitId`. The
  duplicate check is by *line id*, not product. Cannot also be in
  `linesToRemove`, must belong to the sale.
- `linesToRemove`: existing line ids to delete. Must belong to the sale.

`SaleValidator.guardLineIdsBelongToSale` enforces all of the above.
`updateAndSyncReservations` (drafts) keeps stock reservations in sync;
`updateWithoutSyncingReservations` (during confirmation) skips that work
because reservations are about to be cleared wholesale anyway.

### "Surviving lines"

After an update, the **surviving line set** is
`survivingExisting + updatedLines + newLines`. Confirmation requires at
least one surviving line (`guardSurvivingLinesNotEmpty`). The same set
drives discount reconciliation, totals, and Kafka payloads.

---

## 5. Adjustments (Discounts + Surcharges)

Adjustments live in the `sale_adjustment` package but are orchestrated by
`SaleMutator` and are part of the sale contract. An adjustment is either a
**discount** (`direction = DISCOUNT`, code `DISC`) or a **surcharge**
(`direction = SURCHARGE`, code `SRCH`). The two share the same entity
shape, persistence model, lifecycle, and reconciliation logic.

### Shape

- Either **line-level** (`locationProductId != null`) — applies to one line
  identified by product — or **order-level** (`locationProductId == null`).
- `direction`: `DISC` (reduces the payable) or `SRCH` (adds to the
  payable).
- `calculationMethod`: `FIXED_VALUE` (currency amount) or `PERCENTAGE`
  (percent of the line total or the sale subtotal).
- Every adjustment references an `adjustment_reason_id` (org-schema
  lookup). The service validates the reason exists and that
  `AdjustmentReason.canApply(reason, direction)` is true — i.e. a
  `DISCOUNT`-only reason cannot be applied as a surcharge, and vice
  versa. Reasons whose direction is `BOTH` are system-only.
- `note` — optional free text. `approvedById` — user reference, stored
  but not yet permission-gated.
- Frozen amounts: every adjustment entity stores the `calculatedAmount`
  at the time of application. The entity is immutable.

### Validation

- **Line-level adjustments must reference a product that has a line on
  this sale.** `NewSaleAdjustmentValidator.validateNewAdjustments`
  enforces this for incoming and pre-existing adjustments.
- The `AdjustmentReason` must exist and `canApply(reason, direction)`
  must be true (cross-schema lookup via `AdjustmentReasonService`).
- **Discount ceilings** (`guardAdjustmentCeilings`, only enforced when
  `enforceTotals = true`):
  - Sum of line-level discounts on a single line must not exceed that
    line's `lineTotal()` (`guardLineTotals`).
  - Sum of order-level discounts must not exceed
    `subtotal − Σ(existing line-level discounts)` (`guardOrderTotal`).
- **Surcharge ceilings are not enforced in phase 1** — left as
  `TODO(phase 2)` in `NewSaleAdjustmentValidator` and
  `SaleAdjustmentValidator`.
- Ceiling checks fire **only on non-draft paths** — i.e.
  `SaleConfirmationHandler.createSale` and `convertDraftToSale`. In
  `SaleMutator.create` the gate is `enforceTotals = sale.status !=
  DRAFT`; in `SaleMutator.doUpdate` it is a parameter
  (`updateAndSyncReservations` passes `false`,
  `updateWithoutSyncingReservations` passes `true`). Drafts may
  temporarily go upside-down on discounts because totals are still
  being assembled.

### Modifications

- Adjustments can be **included** in a `SaleCreateDto` that starts the
  sale in either `DRAFT` or `CONFIRMED` —
  `SaleAdjustmentService.applyValidatedAdjustments` is unguarded and
  runs as part of the create transaction.
- After creation, adjustments can only be added/removed while the sale
  is in `DRAFT` (`SaleAdjustmentValidator.guardIsDraft` on
  `addAdjustments`, `removeAdjustments`, `removeAdjustmentsByLineIds`).
  Once `CONFIRMED`, adjustments are frozen. Note: `guardIsDraft` reads
  the in-memory `sale.status`, so during `convertDraftToSale` the
  mutator's adjustment add/remove still passes —
  `SaleConfirmationHandler` only flips the status to `CONFIRMED` *after*
  the mutator returns.
- `adjustmentsToRemove` ids must belong to the sale
  (`guardAdjustmentsBelongToSale`).
- Removing a sale line cascades: all adjustments attached to that line
  are removed (`removeAdjustmentsByLineIds`). Because that helper guards
  `DRAFT`-only, **removing a line on any non-draft path is implicitly
  blocked** — there is no flow that mutates lines on a CONFIRMED sale.

### Percentage staleness

If line quantities/units change during a draft update, percentage
adjustments (both discounts and surcharges) whose `calculatedAmount` no
longer matches the recomputed amount are **replaced**
(`SaleAdjustmentReconciler.reconcileAdjustmentsAfterLineChanges`).
Fixed-value adjustments are left untouched.

After reconciliation, `assertAdjustmentsStillFitAfterLineChanges`
ensures the remaining DISCOUNTS still fit under the new totals;
otherwise the update is rejected with an actionable message naming the
offending line. (Surcharges are not bound by ceilings in phase 1, so
they are not re-checked.) **This assertion is gated on the same
`enforceTotals` flag** — it fires on the `convertDraftToSale` path but
**not** on plain draft updates, which may intentionally leave discounts
upside-down until conversion.

### Totals contribution

Adjustments **never modify `sale_line.unitPrice`**. They live as separate
`SaleAdjustmentEntity` rows whose frozen `calculatedAmount` is summed
into **four persisted buckets** on `SaleEntity`:

- `lineLevelDiscountTotal  = Σ(adjustments where direction = DISCOUNT  and saleLineId != null)`
- `orderLevelDiscountTotal = Σ(adjustments where direction = DISCOUNT  and saleLineId == null)`
- `lineLevelSurchargeTotal = Σ(adjustments where direction = SURCHARGE and saleLineId != null)`
- `orderLevelSurchargeTotal= Σ(adjustments where direction = SURCHARGE and saleLineId == null)`

All four are populated together by `SaleMutator.applyTotals`.
`SaleEntity.discountTotal()` returns the sum of the two discount
buckets; `SaleEntity.surchargeTotal()` returns the sum of the two
surcharge buckets (both treating nulls as zero). `payableTotal()`
becomes:

```
payableTotal() = grandTotal ?: subtotal − discountTotal() + surchargeTotal()
```

`grandTotal` is set after taxes finalize (see §7). The price drop /
markup emerges at the sale level via `payableTotal()`, not at the line
level.

`SaleResponseDto` surfaces all four buckets separately so callers can
render the breakdown without re-aggregating the adjustment list.

---

## 6. Stock: Reservations vs FIFO Consumption

Two distinct mechanisms protect inventory:

### Reservations (drafts only)

- `SaleLineStockReservationEntity` holds **`quantity_reserved`** per sale
  line (always written as `line.baseQty()` — i.e. in the product's base
  unit), scoped to a single `locationProductId`.
- A reservation is created when a draft line is created
  (`SaleStockReserver.reserve`), kept in sync on draft updates
  (`syncUpdatedReservations` — only writes when the new `baseQty()` differs
  from the persisted one), and cleared on:
  - Draft confirmation (`clearBySale` before FIFO consumption).
  - Discarding a draft (`SaleUpdater.voidSale` → `DISCARDED` branch).
  - Removing a draft line (`clearByLineIds`).
- Three points guard available stock:
  1. **At create** (draft or direct-confirm), `SaleLinesInsertPreparer`
     calls `SaleValidator.ensureSufficientStockForLines`, which subtracts
     `Σ(all reservations)` from the latest balance. The sale has no
     reservation of its own yet, so no self-exclusion is needed.
  2. **On draft update**, `SaleLinesUpdateApplier` calls
     `SaleValidator.guardStockForDraftUpdates`, which subtracts
     `Σ(reservations from OTHER sales)` so a quantity bump on an existing
     draft line does not double-count its own outstanding reservation.
  3. **On confirmation** (direct or convert-from-draft), FIFO consumption
     itself fails fast if a layer can't be filled.

### FIFO Consumption (confirmation only)

- On confirmation / direct create, `SaleStockUpdater.consumeStock` runs FIFO
  against actual stock layers, keyed by `SaleLineStockRequest(saleLineId,
  locationProductId, baseQuantity, unitId, conversionFactor)`.
- The sale reference number is passed as the movement source so audits can
  trace stock out → sale.
- On `VOIDED`, `SaleStockUpdater.restoreStock` reverses the FIFO consumption
  for that reference.

### Locking discipline

- `EntityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)` is taken
  before reading balances / reservations and before FIFO consumption /
  restoration. This prevents two concurrent transactions on the **same
  product** from each seeing identical "available" stock and oversellling.
  `convertDraftToSale` re-acquires the PRODUCT lock after the mutator
  returns and before `clearBySale` + FIFO, since the mutator's own
  `guardStockForDraftUpdates` runs against the *old* reservation snapshot.
- `EntityAdvisoryLock.acquire(LockNamespaces.SALE, saleId)` is acquired by
  `SaleDataFetcher.lockAndGetSale` / `lockAndGetSaleContext`, which every
  flow that touches an *existing* sale (`updateDraft`,
  `convertDraftToSale`, `voidSale`, `updateNotes`, `updatePaymentStatus`,
  and the standalone payment / payment-void paths) calls first. The two
  *create* entry points (`createDraft`, `createSale`) do not acquire it —
  there is no sale to lock.

### Insufficient stock

Both reservation and consumption paths surface insufficient-stock errors
with the **product label**, available qty (stripped zeros), and requested
qty (`SaleValidator.throwIfOverSelling`). Never expose raw UUIDs to the
user when a label is available.

---

## 7. Taxes (asynchronous, Kafka-driven)

Taxes are **never computed in the sale transaction itself.** Instead:

1. On confirmation, `SaleConfirmedHandlerForKafka.publish` emits
   `SaleConfirmedEvent(subtotal, discountTotal, surchargeTotal, payableTotal,
   lines, adjustments, dateSold,
   saleReferenceNumber, …)`.
2. `SaleTaxFinalizationProcessor.handle` consumes the event in a separate
   transaction:
   - Looks up active `OrgJurisdictionTaxType` rows (status = ACTIVE).
   - Keeps only types with `TaxApplicationLevel.TRANSACTION` and
     `TaxTrigger.SALE`.
   - Finds the active `TaxRate` map for `event.dateSold`. **The rate map is
     keyed by `orgJurisdictionTaxTypeId` (org-row id)** — i.e. lookup is
     `rates[orgTaxType.id]`. The id stored on `TaxEntryEntity` below is
     different: it's the platform-level `jurisdictionTaxTypeId`.
   - Computes `taxAmount` per type (where `rate = ratePercentage / 100` —
     the stored `ratePercentage` is e.g. `7` for 7%, not `0.07`):
     - `PERCENTAGE` + exclusive → `taxableAmount * rate`.
     - `PERCENTAGE` + inclusive → `taxableAmount − taxableAmount / (1+rate)`.
     - `FIXED_VALUE` → `flatAmount` (scale 4, half-up).
   - Persists `TaxEntryEntity` rows keyed by `(sourceReferenceNumber,
     sourceType=SALE, taxTypeId)`. The stored `taxTypeId` is the
     **platform-level** `jurisdictionTaxTypeId` (not the org-jurisdiction
     row id) — that's the value the unique constraint and idempotency
     checks both key on.
   - Updates `sale.taxTotal` and `sale.grandTotal` (= `taxableAmount +
     non-inclusive taxes`).
3. On void, `SaleTaxReversalProcessor.handle` writes negated tax entries
   under `sourceType = SALE_VOID`, keyed to the fiscal period of the void
   date.

### Idempotency rules (Kafka)

- `SaleTaxFinalizationProcessor.shouldProcess` checks
  `taxEntryService.existsBySourceReference(refNum, SALE)`.
- `SaleTaxReversalProcessor.shouldProcess` requires originals to exist
  **and** no reversal to exist yet.
- Database guard: the unique constraint
  `add-unique-constraint-tax-entry-source-ref-type-tax-type` ensures
  retries can never double-insert.

### Consequences for callers

- `SaleEntity.taxTotal` / `grandTotal` are **null at the end of the create
  transaction** and become populated **after the Kafka event lands**. Code
  that needs the final total must call `payableTotal()` (which gracefully
  falls back to `subtotal − discountTotal + surchargeTotal` when `grandTotal` is null).

---

## 8. Payments

Payments live in the `sale_payment` package; the sale package interacts
with `SalePaymentService` only.

### Submitted with the sale

`SaleMutator.recordPayments` runs **inside the same transaction** as the
sale create/update for payments sent in the DTO. Behavior:

- Empty payments list → no-op, sale `paymentStatus` left as-is.
- Each amount must be positive (`SalePaymentValidator.guardPositiveAmount`).
- New status is computed from `paid` vs `total`:
  - `0` → `UNPAID`
  - `paid > total` → `OVERPAID`
  - `paid < total` → `PARTIALLY_SETTLED`
  - else → `FULLY_SETTLED`
- A Kafka `SalePaymentRecordedEvent` is published per call.

### Ceilings

- **For non-draft sales (create/confirm/convert), payments cannot exceed
  the sale total** (`guardPaymentsWithinSaleTotal`). Drafts allow over-paid
  amounts temporarily because totals can still change.
- **For walk-in sales (always non-draft), payments must fully cover the
  total** (`guardFullPaymentCoverage`).
- `payableTotal()` is used as the ceiling. Prior to tax finalization,
  this is `subtotal − discountTotal + surchargeTotal`; after, it is `grandTotal`.

### Standalone payment recording

`SalePaymentService.recordPayment` (post-sale) runs through the
`SaleDataFetcher.lockAndGetSaleContext` lookup, checks `guardOpenForPayment`
(only `CONFIRMED` accepts standalone payments — `DRAFT` is rejected with
"submit payments with the draft itself"; `VOIDED` and `DISCARDED` are also
rejected), and enforces the `amount ≤ saleTotal − alreadyPaid` ceiling.

### Voiding payments

- Cannot void a payment whose sale is already `VOIDED`.
- A payment can only be voided once (`guardNotAlreadyVoided`).
- After voiding, the sale's `paymentStatus` is recomputed from
  `salePaymentFetcher.calculatePaidAmount` (which excludes voids).

---

## 9. Persistence & Transactions

- Every entry point uses `@TransactionalOnLocationSchema` so the schema
  search path is correctly set per location. Read-only flows
  (`SaleDataFetcher.fetchRecent` / `getSaleContactId`, the Kafka handlers'
  `reissue` path) declare `readOnly = true`. The lock-acquiring lookups
  `SaleDataFetcher.lockAndGetSale` / `lockAndGetSaleContext` are annotated
  `Propagation.MANDATORY` — they must join an already-open writable
  transaction from the caller, and the class-level `readOnly` flag does
  not apply to them.
- Sale lines, reservations, discounts, and payments are persisted **in the
  same transaction** as the sale itself. If any guard throws, nothing is
  committed.
- Kafka events are published through `ApplicationEventPublisher` and are
  bound to transaction commit by the messaging infrastructure — events for
  a rolled-back transaction never reach the broker.
- Audit trail: `SaleEntity`, `SaleLineEntity`, and `SaleAdjustmentEntity` are
  `@Audited` (Hibernate Envers). `SaleVoidEntity` and
  `SaleLineStockReservationEntity` are not — they are
  append-only/transient by nature.

---

## 10. Mapping & DTO Conventions

General project rules (Insert flow, `Optional<T>?` partial-update
convention, no `@MappingTarget`, entity rules) live in
`.claude/instructions.md`. Sale-package specifics only:

- Entity helpers in this package: `SaleEntity.payableTotal()`,
  `SaleEntity.discountTotal()`, `SaleEntity.surchargeTotal()`,
  `SaleLineEntity.baseQty()`, `SaleLineEntity.lineTotal()`. No business
  logic beyond these.
- Domain DTOs (`SaleResponseDto`, `SaleLineResponseDto`) abstract DB
  details and are what cross package boundaries.
- API DTOs (`SaleCreateDto`, `SaleUpdateDto`, `SaleLine*Dto`,
  `SaleVoidCreateDto`, `SaleNotesUpdateDto`) are request shapes.
- **`SaleUpdateDto.applyTo` throws on `contactId == Optional.empty()`** —
  a null contact is not legal, so the standard "empty = clear" semantics
  do not apply to that one field.
- Reference numbers are surfaced through `sale.requiredReference()` once
  persisted. The Kafka publishers depend on this — **never publish an
  event with a null reference**.

---

## 11. Kafka & Idempotency

Events produced by this package:

| Event                       | Trigger                                           | Handlers                       |
|-----------------------------|---------------------------------------------------|--------------------------------|
| `SaleConfirmedEvent`        | `createSale`, `convertDraftToSale`                | `SaleTaxFinalizationProcessor` |
| `SaleVoidedEvent`           | `voidSale` (only when CONFIRMED→VOIDED)           | `SaleTaxReversalProcessor`     |
| `SalePaymentRecordedEvent`  | Any payment recorded with the sale or stand-alone | accounting/external systems    |
| `SalePaymentVoidedEvent`    | `SalePaymentService.voidPayment`                  | accounting/external systems    |

The two payment events are **conditional on the payment method carrying an
`accountCode`**. `SalePaymentHandlerForKafka.publish` drops payment lines
whose method has no account code; if every line drops, no event is
published. `SalePaymentVoidHandlerForKafka.publish` similarly skips the
void event when the original payment's method has no account code. The
sale itself is still persisted/voided — only the downstream accounting
fan-out is suppressed.

General Kafka processor rules (`shouldProcess` idempotency check backed by
a unique DB constraint on the natural key, re-entrancy, transactional
boundaries) live in `.claude/instructions.md` under KAFKA. The
sale-package implementation of those rules is:

- `SaleTaxFinalizationProcessor` / `SaleTaxReversalProcessor` key on
  `(sourceReferenceNumber, sourceType, taxTypeId)` and rely on
  `add-unique-constraint-tax-entry-source-ref-type-tax-type`.
- Every Kafka publisher in this package also implements
  `EventReissueHandler` so administrative replays
  (`SaleConfirmedHandlerForKafka.reissue`, `SaleVoidHandlerForKafka.reissue`)
  rebuild the same payload from the database.

---

## 12. Validation Ordering (Performance + UX)

The flows in this package intentionally validate **before** running
expensive operations. When extending them, preserve that ordering:

1. **Pure DTO guards first** (free): walk-in check, "not reassigning to
   walk-in", positive quantities, no duplicate products, no overlap
   between `linesToUpdate` and `linesToRemove`.
2. **Cheap status guards next** (single FK by id):
   `guardIsDraft`, `guardCanVoid`.
3. **Single-row lookups** (contact exists, sale exists).
4. **Bulk fetches** (product summaries, unit conversion graph, balances,
   reservations) — these are the expensive ones and should run only after
   the above pass.
5. **Stock guards** (advisory lock + balances + reservations).
6. **Persist** (entities, reservations, discounts, payments).
7. **Publish Kafka events** (last, so the transaction is shaped correctly).

The default place to add a new guard is **as early as it can correctly
run** — i.e. as soon as the data it needs is in scope.

---

## 13. Quick Reference: Each Public Entry Point

### `SaleDraftHandler.createDraft(SaleCreateDto)`
- Rejects walk-in payloads.
- Requires contact to exist.
- Guards date/fiscal period only if payments are present.
- Creates `SaleEntity` at `DRAFT`, lines, reservations, adjustments
  (no ceiling enforcement), payments.

### `SaleDraftHandler.updateDraft(SaleUpdateDto)`
- Sale must be `DRAFT`.
- Cannot reassign to walk-in.
- Requires contact to exist (after applying header changes).
- Date/fiscal guard only if payments are present.
- Syncs reservations to the new surviving line set.
- Adds/removes adjustments; reconciles percentage adjustments; does
  **not** enforce ceilings (drafts can be upside-down on discounts).

### `SaleConfirmationHandler.createSale(SaleCreateDto)`
- Walk-in is allowed (in fact, walk-in requires this path).
- Contact must exist if non-walk-in.
- `dateSold` defaults to now-org; future date rejected; fiscal period must
  be open.
- Requires ≥1 line (`SaleValidator.guardHasLines` in
  `prepareForSaleConfirmation`).
- Creates `SaleEntity` at `CONFIRMED`, lines, adjustments (discount
  ceilings enforced; no surcharge ceiling in phase 1), payments (within
  total; walk-in requires full coverage).
- Runs FIFO consumption.
- Publishes `SaleConfirmedEvent` → taxes finalize asynchronously.

### `SaleConfirmationHandler.convertDraftToSale(SaleUpdateDto)`
- Sale must be `DRAFT`; cannot reassign to walk-in.
- Must end with ≥1 surviving line.
- Backfills `soldById` / `dateSold` if missing.
- Fiscal period must be open for the final `dateSold`.
- Skips reservation sync (clears reservations after locking products),
  enforces adjustment (discount) + payment ceilings on the surviving lines, runs FIFO
  consumption, publishes `SaleConfirmedEvent`.

### `SaleUpdater.voidSale(SaleVoidCreateDto)`
- DRAFT → DISCARDED (releases reservations only).
- CONFIRMED → VOIDED (restores stock, writes `SaleVoidEntity`, publishes
  `SaleVoidedEvent`).
- Rejected if already voided/discarded or if active payments exist.
- Confirmed-sale void requires the fiscal period **of today** to be open
  (the void itself is the bookable event).
- DB defense-in-depth: `sale_void.sale_id` is `unique = true`, so a second
  void insert for the same sale fails at the constraint even if the
  status guard is somehow bypassed.

### `SaleUpdater.updateNotes(id, notes)` / `updatePaymentStatus(id, status)`
- Both take the SALE advisory lock via `lockAndGetSale` and then assign
  the field directly; no extra validation.
- `updateNotes` is **REST-exposed** (`PUT /secured/sales/{saleId}/notes`
  in `SaleEndpoint`).
- `updatePaymentStatus` is **intra-domain only** — driven by
  `SalePaymentService` (`recordPayment`, `voidPayment`); not wired to any
  REST endpoint.

### `SaleDataFetcher.fetchRecent(n?)`
- Defaults to 10; rejects `n <= 0` and `n > 1000`.
- Sorted by `createdOn DESC`.

### Sellable product lookup (now on the product package)
- Lives at `location_product/api/LocationProductForSaleSearchService`,
  REST-exposed as `POST /secured/location-products/search-for-sale` on
  `LocationProductEndpoint`. The sale package no longer hosts this surface.
- Request body is `PageRequest<SaleProductSearchParameters, String>` —
  the parameter type only exposes `searchText` + `excludeIds` (the wider
  `ProductSearchParameters` filters are intentionally not surfaced).
  Status is forced to `ACTIVE` internally.
- Each row is enriched with `stockBatches`: `StockEntryEntity` rows with
  `quantityRemaining > 0`, sorted by `priority ASC` (nulls last) — the
  FIFO preview order the cashier UI walks as quantities are entered.
- This is an **estimate at query time, not a reservation**. The lookup
  takes no advisory locks; consumption still happens through
  `SaleStockUpdater` at confirmation time under `Propagation.MANDATORY`.

---

## 14. Common Pitfalls

- **Computing `grandTotal` synchronously.** It is null until the tax
  processor runs. Use `payableTotal()` if you need a real number in the
  same transaction.
- **Using `LocalDate.now()` / `ZoneOffset.UTC` anywhere in this package.**
  Always go through `DateTimes`. Sales straddle midnight and member orgs
  span many zones.
- **Adding a guard after a bulk fetch.** Move it earlier. The fetch is
  often the expensive part — fail fast.
- **Mutating a non-draft sale's lines, adjustments, or contact.** All such
  mutations are blocked at the validator layer; do not try to bypass.
- **Forgetting reservation lifecycle on a draft.** If you change line
  base-quantities, you must call `syncUpdatedReservations` (or rely on
  the existing `updateAndSyncReservations` path). Stale reservations are
  the leading source of oversell bugs.
- **Publishing Kafka events outside the sale transaction.** Always go
  through `ApplicationEventPublisher` so the event is bound to commit.
- **Kafka publishers bang (`!!`) on `subtotal` and `dateSold`** in both
  `SaleConfirmedHandlerForKafka.publish` and
  `SaleVoidHandlerForKafka.publishVoid`. The discount and surcharge
  totals are sourced via `sale.discountTotal()` / `sale.surchargeTotal()`
  (each treating both of its bucket columns as zero when null), and
  `payableTotal()` falls back to `subtotal − discountTotal +
  surchargeTotal` when `grandTotal` is null — so a sale with no
  adjustments publishes `0` for the bucket totals rather than NPE-ing.
  Confirmation/void paths always set subtotal and dateSold before
  publish, so those asserts hold in normal flow. Stay aware of the
  contract when introducing new mutation paths or admin tools — anything
  that produces a sale entity without subtotal/dateSold populated will
  NPE on publish/republish.

---

## 15. Where Things Live

| Topic                      | Class(es)                                                                                               |
|----------------------------|---------------------------------------------------------------------------------------------------------|
| State, header, totals      | `SaleEntity`, `SaleStatus`, `SaleStatusConverter`                                                       |
| Lines                      | `SaleLineEntity`, `SaleLineMapper`, `SaleLines*Preparer`, `SaleLines*Context`, `SaleLinesUpdateApplier` |
| Validation                 | `SaleValidator`, `SaleAdjustmentValidator`, `NewSaleAdjustmentValidator`, `SaleAdjustmentReconciler`    |
| Stock reservation          | `SaleStockReserver`, `SaleLineStockReservationEntity`, `StockReservationDtos`                           |
| Adjustments (disc + srch)  | `sale_adjustment/` package (entity, repo, service, validators, reconciler, fetcher, calculator)         |
| Adjustment reasons         | `organizations/business/adjustment_reason/` (org schema lookup; seeded via `AdjustmentReasonSeeder`)    |
| Payments                   | `sale_payment/` package                                                                                 |
| Taxes (async finalization) | `SaleTaxFinalizationProcessor`, `SaleTaxReversalProcessor`, `tax_entry/` package                        |
| Kafka publish/republish    | `SaleConfirmedHandlerForKafka`, `SaleVoidHandlerForKafka`                                               |
| Void                       | `SaleUpdater.voidSale`, `SaleVoidEntity`, `SaleVoidRepository`                                          |
| Read APIs                  | `SaleDataFetcher`, `SaleAssembler`                                                                      |

Keep this table accurate as the package evolves — it is the entry point
for anyone (or any agent) doing a first-pass investigation.
