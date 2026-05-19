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

| Class                  | Purpose                                                                    |
|------------------------|----------------------------------------------------------------------------|
| `DraftSalePersister`   | Persist a sale at `DRAFT` from a `SaleSaveRequest` (called by sale_session) |
| `ConfirmedSalePersister` | Persist a sale at `CONFIRMED` from a `SaleSaveRequest` (FIFO + Kafka)       |
| `SaleUpdater`          | Void a sale; update payment status; update notes                            |
| `SaleDataFetcher`      | Read sales (recent list, context lookup, header/line snapshots, contact, commit-time draft+version load) |

Creating and confirming a sale is **only** possible via a session — see
`sale_session/README.md`. The persisters are the seams the session layer calls
into; they do not have REST endpoints of their own.

Sellable product lookup (product + per‑product FIFO stock batch previews
for the sale entry screen) lives on the **product** package, not here —
see `location_product/api/LocationProductForSaleDataFetcher` and the
`POST /secured/location-products/search-for-sale` endpoint on
`LocationProductEndpoint`.

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

- A sale **starts** at `DRAFT` (via `DraftSalePersister.saveDraft`) or jumps
  straight to `CONFIRMED` (via `ConfirmedSalePersister.confirm`). Both are
  called by `SaleSessionPersister` and are **never** invoked by REST
  directly.
- Mutation of a `DRAFT` happens through a sale session — header changes,
  line/adjustment/payment changes, then
  `POST /secured/sale-sessions/{sessionId}/draft` re-persists the existing
  draft. Optimistic version check (`@Version` on `SaleEntity`) rejects
  concurrent saves.
- A `DRAFT` becomes `CONFIRMED` only through
  `ConfirmedSalePersister.confirm`.
- Voiding a `DRAFT` transitions it to `DISCARDED` (reservations are released,
  no stock movement, no Kafka void event — the sale never affected stock).
- Voiding a `CONFIRMED` sale transitions it to `VOIDED` (stock is restored,
  tax entries reversed via Kafka, `SaleVoidEntity` recorded).
- `VOIDED` and `DISCARDED` are terminal. `SaleValidator.guardCanVoid` rejects
  both, and also rejects voiding when there are active (non-voided) payments.

`SaleStatus` is persisted as a short code via `SaleStatusConverter`
(`DFT`, `CFM`, `VD`, `DSC`); the `status` column is `length = 5`. Never
`@Enumerated`.

---

## 3. Identity: Contact, SoldBy, DateSold

### contactId

- A sale is always tied to **exactly one contact**. Walk-in sales use
  `SystemContact.WALK_IN.id`.
- A draft can be reassigned via `SaleSessionHeaderHandler.update`, which calls
  `ContactService.guardExists` for any non-walk-in contact. There is no
  hard reassignment rule against walk-in for drafts, but **a walk-in must be
  fully paid at confirm time** (`SaleSessionPersister.confirm` calls
  `SaleSessionValidator.guardWalkInFullyCovered`).
- For any non-walk-in sale, `ContactService.guardExists` must succeed before
  persistence. The check is skipped entirely for walk-in (no DB lookup),
  which is why `SystemContact.WALK_IN.id` is not required to exist as a
  regular contact row.

### soldById

- Optional on `DRAFT`. On confirmation, it is auto-filled from
  `SessionContextProvider.getUserId()` if absent.
- Stored on the sale; surfaced via `userQualifier.getUserFullName` in
  responses.

### dateSold

- Optional on `DRAFT`.
- On confirmation, missing `dateSold` defaults to
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
- `unitPrice` is **captured from the product at line add time** (when the
  user adds the line to a session) and frozen for the life of that line —
  a price change on the product does not retro-affect existing lines.
- `lineTotal() = quantity * unitPrice` (scale 4, half-up via `Decimals`).
- `baseQty() = quantity * conversionFactor` (scale 4).

### Per-line invariants

1. `quantity` must be **strictly positive** (guarded by `SaleSessionValidator`
   on every mutation).
2. **No duplicate products** in the same sale — guarded on every session
   mutation.
3. `locationProductId` must be **active** (`LocationProductService.guardAllActive`)
   when adding a line to a session. A product deactivated after it has been
   put on the cart does **not** block subsequent draft saves or confirm —
   the line stays valid once it has been accepted.
4. `unitId` must be a valid unit reachable in the `UnitConversionGraph` to
   the product's base unit; otherwise `getFactor` throws.
5. `unitPrice` must be set on the product summary at add time; the session
   line handler rejects with `"Product X has no unit price"` otherwise.

### Sync semantics at commit

`SaleLineSync.sync` synchronises the persisted line set against the
session's line set:

- `existingId == null` → INSERT new line entity, return mapping
  `clientKey → newId`.
- `existingId != null` and present in DB → UPDATE in place.
- Existing line not in incoming set → DELETE (cascade: reservations were
  cleared for the whole sale first).

After sync, fresh reservations are issued for the surviving line set.

---

## 5. Adjustments (Discounts + Surcharges)

Adjustments live in the `sale_adjustment` package. An adjustment is either a
**discount** (`direction = DISCOUNT`, code `DISC`) or a **surcharge**
(`direction = SURCHARGE`, code `SRCH`). The two share the same entity shape,
persistence model, lifecycle, and reconciliation logic.

### Shape

- Either **line-level** (references a `SessionIdentity` of a session line —
  carries through to a `sale_line_id` after commit) or **order-level**
  (no `lineId`).
- `direction`: `DISC` (reduces the payable) or `SRCH` (adds to the payable).
- `calculationMethod`: `FIXED_VALUE` (currency amount) or `PERCENTAGE`
  (percent of the line total or the sale subtotal).
- Every adjustment references an `adjustment_reason_id` (org-schema lookup).
  The session validator validates the reason exists and that
  `AdjustmentReasonService.requireCanApply(reasonId, direction)` succeeds.
- `note` — optional free text. `approvedById` — user reference, stored
  but not yet permission-gated.
- Frozen amounts: `calculatedAmount` is recomputed by the session totals
  calculator after every mutation but only **persisted** to the entity at
  commit.

### Validation

- **Line-level adjustments must reference a session line.**
  `SaleSessionValidator.guardAdjustmentReferences` enforces this on every
  mutation.
- The `AdjustmentReason` must exist and `canApply(reason, direction)`
  must be true — `requireCanApply(reasonId, direction)` overload keeps
  cross-domain `AdjustmentReasonDto` access out of the session layer.
- **Discount ceilings are enforced on every mutation**:
  - Sum of line-level discounts on a single line must not exceed that
    line's `lineTotal()`.
  - Sum of order-level discounts must not exceed
    `subtotal − Σ(line-level discounts)`.
- **Surcharge ceilings are not enforced in phase 1**.

### Sync semantics at commit

`SaleAdjustmentSyncer.sync` reconciles the adjustment set against the
incoming input:

- Existing adjustments not in the incoming set → DELETE.
- Existing adjustments in the incoming set → UPDATE in place:
  `calculatedAmount` and `saleLineId` are recomputed against the
  just-persisted lines so a kept percentage adjustment reflects the
  current subtotal / line total, not the value it was first inserted
  with. All other fields (`value`, `direction`, `calculationMethod`,
  `adjustmentReasonId`, `note`, `approvedById`) remain `updatable = false`
  at the column level and never change after insert.
- New adjustments (`existingId == null`) → INSERT, with `calculatedAmount`
  computed against the persisted lines.

Row IDs are stable across saves — kept adjustments retain their original
id, so the session's `existingId`/`saleAdjustmentIdsByClientKey` mapping
survives any number of draft saves.

### Totals contribution

Adjustments **never modify `sale_line.unitPrice`**. They live as separate
`SaleAdjustmentEntity` rows whose frozen `calculatedAmount` is summed into
**four persisted buckets** on `SaleEntity` (populated by
`SaleTotalsApplier`):

- `lineLevelDiscountTotal  = Σ(direction = DISCOUNT  and saleLineId != null)`
- `orderLevelDiscountTotal = Σ(direction = DISCOUNT  and saleLineId == null)`
- `lineLevelSurchargeTotal = Σ(direction = SURCHARGE and saleLineId != null)`
- `orderLevelSurchargeTotal= Σ(direction = SURCHARGE and saleLineId == null)`

`SaleEntity.payableTotal()` becomes:

```
payableTotal() = grandTotal ?: subtotal − discountTotal() + surchargeTotal()
```

`grandTotal` is set after taxes finalize (see §7).

---

## 6. Stock: Reservations vs FIFO Consumption

Two distinct mechanisms protect inventory:

### Reservations (drafts only)

- `SaleLineStockReservationEntity` holds **`quantity_reserved`** per sale
  line (always written as `line.baseQty()`).
- Every `DraftSalePersister.saveDraft` invocation clears all reservations
  for the sale (`clearBySale`, immediately after the line sync) and then
  re-issues fresh reservations for the surviving line set. This avoids
  hand-rolled diffing.
- Reservations are also cleared when:
  - A draft is voided (`SaleUpdater.voidSale` → `DISCARDED` branch).
  - The sale is confirmed (`ConfirmedSalePersister.confirm` clears under the
    PRODUCT advisory lock before FIFO consumption).
- Stock guards run at:
  1. **Add line to session** — `SaleSessionLineHandler.addLine` calls
     `LocationProductService.guardAllActive` (live stock is NOT pre-checked
     here; it gets checked at the next commit).
  2. **Save draft commit** — `SaleValidator.guardStockForDraftUpdates` runs
     after reservations were cleared and lines persisted; computes available
     stock excluding *this* sale's reservations, then issues new ones.
  3. **Confirm** — FIFO consumption fails fast if a layer cannot fill.

### FIFO Consumption (confirmation only)

- On confirmation, `SaleStockUpdater.consumeStock` runs FIFO against actual
  stock layers, keyed by `SaleLineStockRequest`.
- The sale reference number is passed as the movement source so audits can
  trace stock out → sale.
- On `VOIDED`, `SaleStockUpdater.restoreStock` reverses the FIFO consumption
  for that reference.

### Locking discipline

- `EntityAdvisoryLock.acquire(LockNamespaces.PRODUCT, productIds)` is taken
  inside `ConfirmedSalePersister` before clearing reservations + FIFO. The
  draft path lets the validator's own product-scoped lock cover stock
  reads + writes.
- `EntityAdvisoryLock.acquire(LockNamespaces.SALE, saleId)` is acquired by
  `SaleDataFetcher.lockAndGetSale` / `lockAndGetSaleContext`, which every
  flow that touches an *existing* sale (`voidSale`, `updateNotes`,
  `updatePaymentStatus`, and the standalone payment / payment-void paths)
  calls first.

### Insufficient stock

Both reservation and consumption paths surface insufficient-stock errors
with the **product label** (when available), available qty (stripped zeros),
and requested qty (`SaleValidator.throwIfOverSelling`).

---

## 7. Taxes (asynchronous, Kafka-driven)

Taxes are **never computed in the sale transaction itself.** Instead:

1. On confirmation, `SaleConfirmedHandlerForKafka.publish` emits
   `SaleConfirmedEvent(sourceDocumentId, contactId, saleReferenceNumber,
   payableTotal, dateSold)`. The event carries `payableTotal` only —
   line/adjustment detail is not on the wire, so the downstream tax
   processor cannot reach back into line breakdowns; it computes against
   `payableTotal` as the taxable amount.
2. `SaleTaxFinalizationProcessor.handle` consumes the event in a separate
   transaction:
   - Looks up active `OrgJurisdictionTaxType` rows (status = ACTIVE).
   - Keeps only types with `TaxApplicationLevel.TRANSACTION` and
     `TaxTrigger.SALE`.
   - Finds the active `TaxRate` map for `event.dateSold`. **The rate map is
     keyed by `orgJurisdictionTaxTypeId` (org-row id)** — i.e. lookup is
     `rates[orgTaxType.id]`. The id stored on `TaxEntryEntity` below is
     different: it's the platform-level `jurisdictionTaxTypeId`.
   - Computes `taxAmount` per type (where `rate = ratePercentage / 100`):
     - `PERCENTAGE` + exclusive → `taxableAmount * rate`.
     - `PERCENTAGE` + inclusive → `taxableAmount − taxableAmount / (1+rate)`.
     - `FIXED_VALUE` → `flatAmount`.
   - Persists `TaxEntryEntity` rows keyed by `(sourceReferenceNumber,
     sourceType=SALE, taxTypeId)`.
   - Updates `sale.taxTotal` and `sale.grandTotal`.
3. On void, `SaleTaxReversalProcessor.handle` writes negated tax entries
   under `sourceType = SALE_VOID`.

### Idempotency rules (Kafka)

- `SaleTaxFinalizationProcessor.shouldProcess` checks
  `taxEntryService.existsBySourceReference(refNum, SALE)`.
- `SaleTaxReversalProcessor.shouldProcess` requires originals to exist
  **and** no reversal to exist yet.
- Database guard: the unique constraint
  `add-unique-constraint-tax-entry-source-ref-type-tax-type` ensures
  retries can never double-insert.

### Consequences for callers

- `SaleEntity.taxTotal` / `grandTotal` are **null at the end of the commit
  transaction** and become populated **after the Kafka event lands**. Code
  that needs the final total must call `payableTotal()` (which gracefully
  falls back to `subtotal − discountTotal + surchargeTotal` when `grandTotal`
  is null).

---

## 8. Payments

Payments live in the `sale_payment` package; the sale package interacts with
`SalePaymentAppender` (sale_payment/api) at commit time and with
`SalePaymentService` for standalone payments after confirmation.

### Submitted with the commit

`SalePaymentAppender.appendNew` runs **inside the same transaction** as the
sale commit:

- Empty payments list → no-op, sale `paymentStatus` left as-is.
- Each amount must be positive (`SalePaymentValidator.guardPositiveAmount`).
- New `SalePaymentEntity` rows are inserted only for payments whose
  `existingId == null`. Persisted payments cannot be removed via session.
- Resulting status is computed from `paid` vs `total`:
  - `0` → `UNPAID`
  - `paid > total` → `OVERPAID`
  - `paid < total` → `PARTIALLY_SETTLED`
  - else → `FULLY_SETTLED`
- A Kafka `SalePaymentRecordedEvent` is published per call.

### Ceilings

- **At confirm, payments cannot exceed payable total** — enforced by
  `SaleSessionValidator.guardPaymentsWithinTotal`, called from
  `SaleSessionPersister.confirm` before the commit transaction opens.
  **Drafts intentionally permit overpayment** (prepayment is legitimate
  before goods are issued).
- **For walk-in sales, payments must fully cover the total at confirm**
  (`SaleSessionValidator.guardWalkInFullyCovered`, also called from the
  session commit handler).
- At session-mutation time the validator lets payments slide so the user
  can still see what they've staged; both ceilings only apply at confirm.

### Standalone payment recording

`SalePaymentService.recordPayment` (post-confirm) runs through
`SaleDataFetcher.lockAndGetSaleContext`, checks `guardOpenForPayment`
(only `CONFIRMED` accepts standalone payments — `DRAFT` is rejected with
"submit payments with the draft itself"; `VOIDED` and `DISCARDED` are also
rejected), and enforces the `amount ≤ saleTotal − alreadyPaid` ceiling.

### Voiding payments

- Cannot void a payment whose sale is already `VOIDED`.
- A payment can only be voided once (`guardNotAlreadyVoided`).
- After voiding, the sale's `paymentStatus` is recomputed from
  `salePaymentFetcher.calculatePaidAmount`.

---

## 9. Persistence & Transactions

- Every entry point uses `@TransactionalOnLocationSchema` so the schema
  search path is correctly set per location. Read-only flows
  (`SaleDataFetcher.fetchRecent` / `getSaleContactId`, the Kafka handlers'
  `reissue` path) declare `readOnly = true`. The lock-acquiring lookups
  `SaleDataFetcher.lockAndGetSale` / `lockAndGetSaleContext` and the
  commit-time `SaleDataFetcher.loadDraftAtVersion` are annotated
  `Propagation.MANDATORY` — they must join an already-open writable
  transaction from the caller.
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
- `SaleEntity.version` is `@Version`-managed; the session captures it on
  load and `DraftSalePersister` / `ConfirmedSalePersister` verify it before
  saving.

---

## 10. Mapping & DTO Conventions

General project rules (Insert flow, `Optional<T>?` partial-update
convention, no `@MappingTarget`, entity rules) live in
`.claude/instructions.md`. Sale-package specifics only:

- Entity helpers in this package: `SaleEntity.payableTotal()`,
  `SaleLineEntity.baseQty()`, `SaleLineEntity.lineTotal()`. No business
  logic beyond these.
- Domain DTOs (`SaleSummary`, `SaleLineDto`) abstract DB
  details and are what cross package boundaries on the read path.
- Save-request DTOs (`SaleSaveRequest`, `SaleLineSaveRequest`, etc.) abstract
  what the session passes into the persisters. They are the only request
  shape the persisters accept.
- Reference numbers are surfaced through `sale.requiredReference()` once
  persisted. The Kafka publishers depend on this — **never publish an
  event with a null reference**.

---

## 11. Kafka & Idempotency

Events produced by this package:

| Event                       | Trigger                                                | Handlers                       |
|-----------------------------|--------------------------------------------------------|--------------------------------|
| `SaleConfirmedEvent`        | `ConfirmedSalePersister.confirm`                         | `SaleTaxFinalizationProcessor` |
| `SaleVoidedEvent`           | `voidSale` (only when CONFIRMED→VOIDED)                | `SaleTaxReversalProcessor`     |
| `SalePaymentRecordedEvent`  | Any payment recorded with the commit or stand-alone    | accounting/external systems    |
| `SalePaymentVoidedEvent`    | `SalePaymentService.voidPayment`                       | accounting/external systems    |

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

`SaleSessionValidator` runs structural validation on every mutation. The
persisters run only **live-state** guards because they race against
real-world change that has happened since the session was populated. When
extending either side, preserve the priority order:

1. **Pure DTO / session-state guards** (free): positive quantities, no
   duplicate products, line refs valid, payment positivity, discount ceilings.
2. **Cheap status guards** (single FK by id): walk-in coverage at confirm,
   payment ceilings at commit.
3. **Single-row lookups** (contact exists, fiscal period open).
4. **Bulk fetches** (product summaries, unit conversion graph, balances,
   reservations).
5. **Stock guards** (advisory lock + balances + reservations).
6. **Persist** (entities, reservations, discounts, payments).
7. **Publish Kafka events** (last, so the transaction is shaped correctly).

The default place to add a new guard is **as early as it can correctly
run** — i.e. as soon as the data it needs is in scope.

---

## 13. Quick Reference: Each Public Entry Point

### `DraftSalePersister.saveDraft(SaleSaveRequest): SaleSaveResult`
- Guards products active.
- Guards fiscal period + future-date only when payments are present.
- Optimistic version check against `SaleEntity.version`.
- Persists `SaleEntity` at `DRAFT`, syncs lines, clears + re-issues
  reservations (with `guardStockForDraftUpdates` in between), syncs
  adjustments, applies totals, then appends new payments.

### `ConfirmedSalePersister.confirm(SaleSaveRequest): SaleSaveResult`
- Walk-in is allowed (and required to be fully paid).
- Requires ≥1 line.
- Defaults `dateSold` to now-org if absent; future-date rejected; fiscal
  period must be open.
- Acquires PRODUCT lock, clears reservations, runs FIFO consumption.
- Publishes `SaleConfirmedEvent` → taxes finalize asynchronously.

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

### `SaleDataFetcher.getSaleHeader(saleId)` / `getSaleLines(saleId)`
- Used by `SaleSessionLoader` to map an existing draft into a session.
- Snapshots are plain data classes in `sale/api/` (`SaleHeaderDto`,
  `SaleLineDto`) — they do not leak `SaleEntity` / `SaleLineEntity` across
  the package boundary.

### Sellable product lookup (now on the product package)
- Lives at `location_product/api/LocationProductForSaleDataFetcher`,
  REST-exposed as `POST /secured/location-products/search-for-sale` on
  `LocationProductEndpoint`.

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
- **Bypassing the session for line / adjustment edits.** All such edits
  must go through a session — there is no REST surface to mutate a draft
  directly anymore.
- **Forgetting to clear reservations before re-issuing them at commit.**
  `DraftSalePersister` always `clearBySale` first; relying on diff-based
  reservation logic is the leading source of oversell bugs.
- **Publishing Kafka events outside the sale transaction.** Always go
  through `ApplicationEventPublisher` so the event is bound to commit.
- **Kafka publishers bang (`!!`) on `sale.id` and `sale.dateSold`** in
  both `SaleConfirmedHandlerForKafka.publish` and
  `SaleVoidHandlerForKafka.publishVoid`. Confirmation/void paths always
  set `dateSold` before publish, so the assert holds in normal flow.
  Stay aware of the contract when introducing new mutation paths.

---

## 15. Where Things Live

| Topic                      | Class(es)                                                                                               |
|----------------------------|---------------------------------------------------------------------------------------------------------|
| State, header, totals      | `SaleEntity`, `SaleStatus`, `SaleStatusConverter`, `SaleTotalsApplier`                                  |
| Lines                      | `SaleLineEntity`, `SaleLineMapper`, `SaleLineSync`                                                |
| Validation                 | `SaleValidator`                                                                                         |
| Save primitives            | `SaleSaveRequest`, `DraftSalePersister`, `ConfirmedSalePersister`, `SaleSaveFinalizer`                  |
| Stock reservation          | `SaleStockReserver`, `SaleLineStockReservationEntity`, `StockReservationDtos`                           |
| Adjustments (disc + srch)  | `sale_adjustment/` package (`SaleAdjustmentEntity`, `SaleAdjustmentRepository`, `SaleAdjustmentSyncer`, `SaleAdjustmentFetcher`, `AdjustmentAmountCalculator`) |
| Adjustment reasons         | `organizations/business/adjustment_reason/` (org schema lookup; seeded via `AdjustmentReasonSeeder`)    |
| Payments                   | `sale_payment/` package (`SalePaymentAppender` for commit-time inserts)                                |
| Taxes (async finalization) | `SaleTaxFinalizationProcessor`, `SaleTaxReversalProcessor`, `tax_entry/` package                        |
| Kafka publish/republish    | `SaleConfirmedHandlerForKafka`, `SaleVoidHandlerForKafka`                                               |
| Void                       | `SaleUpdater.voidSale`, `SaleVoidEntity`, `SaleVoidRepository`                                          |
| Read APIs                  | `SaleDataFetcher`, `SaleAssembler`                                                                      |
| Session orchestration      | `sale_session/` package (see `sale_session/README.md`)                                                  |

Keep this table accurate as the package evolves — it is the entry point
for anyone (or any agent) doing a first-pass investigation.
