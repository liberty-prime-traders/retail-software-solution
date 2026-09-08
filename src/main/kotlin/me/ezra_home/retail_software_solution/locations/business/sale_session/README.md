# Sale Session Package — Rules & Expectations

This document is the canonical reference for **how a sale session behaves** in
RTSS. A sale session is the **only** path to creating or confirming a sale.

If you are about to change behavior inside this package, scan this file first
and update it when the rule changes.

---

## 1. Scope & Public Surface

The package lives at:

```
locations/business/sale_session/
locations/business/sale_session/api/
```

Public surface (everything else in this package is internal):

| Class                          | Purpose                                                                      |
|--------------------------------|------------------------------------------------------------------------------|
| `SaleSessionHandler`           | Start, abandon, list, get a session                                          |
| `SaleSessionHeaderHandler`     | Patch contactId, soldById, dateSold, notes                                   |
| `SaleSessionLineHandler`       | Add, update, remove session lines                                            |
| `SaleSessionAdjustmentHandler` | Add, remove session adjustments                                              |
| `SaleSessionPaymentHandler`    | Add, remove session payments                                                 |
| `SaleSessionPersister`         | `saveDraft`, `confirm`, `voidSale` — server-side commit / terminal pipelines |

Internal collaborators (do not call from outside `sale_session`):

- `SaleSessionStore` — storage interface; `SaleSessionRedisStore`
  (`redis-active` profile) or `SaleSessionInMemoryStore` (default —
  `!redis-active`)
- `SaleSessionLoader` — `loadFromSale` rehydrates an existing `SaleEntity` into
  the session shape via `DomainToSessionMapper`; `newSession` mints a fresh
  empty session
- `SaleSessionAssembler` — orchestrates response/summary DTO assembly;
  delegates per-bean translation to `SessionToResponseMapper` (MapStruct)
- `SaleSessionValidator` — runs structural validation on every mutation
- `SaleSessionTotalsCalculator` — recomputes totals after every mutation
- `SaleSessionUpdateFinalizer` — touch → recompute → validate → save → buildResponse
  pipeline invoked by every mutation handler

Architectural boundary:

```
sale_session/ → may call sale/api/, sale_adjustment/api/, sale_payment/api/, stock/api/
sale/         → knows nothing about sale_session (convention)
```

`sale_session → stock/api/` is used by `SaleSessionStockOverlay` to populate the
three per-line quantities (`quantityOnHand`, `quantityReserved`,
`quantityAvailable`). It calls `StockAvailabilityFetcher.fetch` (shared with
`LocationProductForSaleAssembler` and `stock_transfer`'s
`ReconciledTransferLineFetcher.draftLines`) for the sale-agnostic baseline —
on-hand balance netted against the **total** of live reservations, no
exclusion — then separately calls `StockReserver.loadReservationBreakdown`
itself to read this session's own reservation via
`ProductReservations.forSale(session.saleId)` and adds it back onto
`quantityReserved`/`quantityAvailable`, so editing a line in a DRAFT session
does not count its own prior reservation against itself. This is a second,
accepted duplicate `loadReservationBreakdown` call (one inside the shared
fetcher, one local to the overlay) — the same tradeoff `stock_transfer`'s
README documents for `guardSufficientStock`/`consumeStockForDispatch`. The
overlay is advisory only — it never throws. Session mutations and loads run
it so the UI can warn the cashier when a requested quantity exceeds
availability; the hard reject happens at `saveDraft` / `confirm` via
`SaleValidator.guardSufficientStockForSale`.

`SaleSessionLineHandler.removeLine` is a partial exception to the buffered
model: when a removed line is persisted (`identity.id != null`), the handler
eagerly calls `StockReserver.clearBySaleLineIds(...)` so the stock hold is
released immediately rather than waiting for `saveDraft`. This prevents
phantom reservations from blocking other customers while the cashier hasn't
yet committed the edit. The `SaleLineEntity` itself stays buffered — it
gets dropped at `saveDraft` via `SaleLineSync`. Consequence: abandoning a
session after `removeLine` leaves the line in DB but with no reservation;
re-opening the sale will show the line again and `saveDraft` will re-reserve
fresh (or fail with insufficient stock if another sale claimed it in the
interim).

`ArchitectureTest` enforces the `.api` boundary symmetrically — every domain's
non-`api` classes are package-private to that domain. The "sale never imports
sale_session" direction is a convention upheld by review, not by the test.

---

## 2. Lifecycle

```
[New Sale btn] ──► SaleSessionHandler.start({contactId})
[Edit Sale btn] ──► SaleSessionHandler.start({contactId, saleId})
                          │
                          ▼
        ┌────────── session in Redis (TTL 2h) ──────────┐
        │  Mutations stay in Redis. Each mutation:       │
        │   1. load session                              │
        │   2. apply edit                                │
        │   3. recompute totals                          │
        │   4. validate structurally                     │
        │   5. save session                              │
        └─────────────────────┬──────────────────────────┘
                              │
      ┌────────────────────┬────────────────────┬────────────────────┐
      │                    │                    │                    │
      ▼                    ▼                    ▼                    ▼
POST {id}/draft     POST {id}/confirm    POST {id}/payments     POST {id}/void     DELETE {id}
DraftSalePersister  ConfirmedSalePersister (CONFIRMED branch:    SaleUpdater.       (abandon)
(persists at        (persists at          SalePaymentService    voidSale           drops session,
 DRAFT status;       CONFIRMED status;    .recordPayment        (DRAFT→DISCARDED  no DB writes
 session lives on)   then deletes         per add call)         or CONFIRMED→
                     session)                                   VOIDED; then
                                                                deletes session)
```

`saveDraft` flushes pending state to the DB and keeps the session alive for
further edits; only `confirm` and `abandon` actually terminate the session.

`SaleSessionHandler.start` short-circuits when an active session already exists
for the supplied `saleId` — returns the existing one (one-session-per-sale
guarantee).

Loading is **status-agnostic** — sales in any state (DRAFT, CONFIRMED, VOIDED,
DISCARDED) can be loaded into a session for display. The session records the
sale's status at load time as `originalStatus`, which gates what mutations the
handlers allow:

| `originalStatus` | Lines / adjustments / header | Add payment                      | Remove payment                                                              | Save path              |
|------------------|------------------------------|----------------------------------|-----------------------------------------------------------------------------|------------------------|
| `DRAFT`          | ✅ allowed                    | ✅ allowed (buffered)             | ✅ transient: dropped; persisted: voided (row stays, marked)                 | `saveDraft`, `confirm` |
| `CONFIRMED`      | ❌ rejected                   | ✅ allowed (recorded immediately) | ✅ persisted only: voided (row stays, marked)                                | none (no commit step)  |
| `VOIDED`         | ❌ rejected                   | ❌ rejected                       | transient: n/a (unreachable); persisted: rejected (`guardSaleNotVoided`)    | none (view-only)       |
| `DISCARDED`      | ❌ rejected                   | ❌ rejected                       | transient: n/a (unreachable); persisted: ⚠️ leaks through (see prose below) | none (view-only)       |

`SaleSessionValidator.guardMutable` enforces the line/adjustment/header rules
and `SaleSessionValidator.canAddPayments` gates add-payment. The commit
handler calls `guardMutable` before either commit path. There is no status
guard on the transient-discard branch: transient payments only exist on
DRAFT sessions by construction (CONFIRMED-session adds short-circuit to
`persisted` identities, and VOIDED/DISCARDED sessions reject `add` outright),
so the transient drop branch is only reachable on DRAFT.

The single `DELETE /payments` endpoint handles both removal cases: when the
payment's `SessionIdentity` is `transient` the handler drops it from the
session in-place; when it's `persisted` the handler forwards to
`SalePaymentService.voidPayment` (a `voidReason` is required in that case),
which in turn calls `SalePaymentValidator.guardSaleNotVoided`. That keeps the
frontend wire-protocol uniform — server decides which side to take based on
the payment's identity. Note `guardSaleNotVoided` only checks the VOIDED
status; DISCARDED is not blocked there, so removing a persisted payment from
a DISCARDED-loaded session is not currently rejected on the service side.

For new sales (no `saleId`), `originalStatus` is seeded to `DRAFT`.

CONFIRMED-session payments **short-circuit**: `SaleSessionPaymentHandler.add`
calls `SalePaymentService.recordPayment` immediately, then folds the persisted
payment back into the session so the cashier's view stays in sync. There is no
separate commit step — every CONFIRMED-session payment exists in the DB the
moment the handler returns. DRAFT sessions still buffer payments (the sale may
not yet exist) and flush them at `saveDraft` / `confirm`. Removing a persisted
payment from a CONFIRMED session routes through `SalePaymentService.voidPayment`
with a `voidReason`.

---

## 3. SessionIdentity

Every row in a session (line, adjustment, payment) carries a
`SessionIdentity` exposed on the row as `identity` (not `id` — the field is
deliberately named after its type so callers don't confuse it with a plain
UUID). Adjustments also carry a `saleSessionLineIdentity: SessionIdentity?`
pointing at the sale session line they're attached to (null = order-level).

```
SessionIdentity
  ├── id: UUID?          — set when the row exists in DB
  └── transientId: UUID? — set when the row only exists in Redis
```

- Exactly one is non-null — enforced by the constructor.
- Frontend echoes it back as-is when targeting a row.
- On commit:
  - `id != null`:
    - **lines** are updated in place — `SaleLineSync` writes the
      current session values for `locationProductId`, `quantity`, `unitId`,
      `unitPrice`, `conversionRatio` onto the existing entity.
      `locationProductId` never changes (no session handler mutates it);
      `unitPrice` is recalculated by `SaleSessionLineHandler.applyLineChanges`
      whenever an update entry edits the line (see Pitfalls §8).
    - **adjustments** and **payments** are kept untouched — there are no
      "update" handlers for these in-session, so existing rows can only be
      retained or removed/voided, never mutated
  - `transientId != null` → insert a new row, flip `transientId` to `id` after.
    For adjustments, `applySaleSaveResult` also flips `relatedSaleLineIdentity`
    from transient to persisted when the referenced line was newly inserted in
    the same commit, keeping adjustment→line references consistent after save.
  - Adjustments missing from the input are deleted by `SaleAdjustmentSyncer`.
    Payments work differently: `SalePaymentAppender.appendNew` only ever
    inserts new (`existingId == null`) payments; persisted payments are
    neither updated nor deleted at commit. The supported way to reverse a
    posted payment is `DELETE /payments` with a `voidReason`, which goes
    through `SalePaymentService.voidPayment` and marks the row voided
    in-place — the session keeps the payment with `voidedReason` set rather
    than removing it from its list.

`SessionIdentity.key()` returns whichever UUID is set; use it as a lookup key
inside the session.

---

## 4. Storage

`sessionId` is a `UUID` end-to-end — generated by `SaleSessionHandler.start`,
echoed by every endpoint, and stored on `SaleSession.sessionId`. The Redis
keys below string-encode the UUID at the boundary (`.toString()`) and decode
on read (`UUID.fromString`).

Up to two Redis keys per session plus a global index:

```
sale-session:{sessionId}      → JSON-serialized SaleSession (TTL 7200s, hard-coded)
sale-session:by-sale:{saleId} → sessionId text (set only when session.saleId != null)
sale-session:open             → SET of sessionId text driving `listOpenSessions()`
```

`SaleSessionRedisStore` keeps all three in lockstep on every save / delete.
`SaleSessionInMemoryStore` is API-equivalent for tests / local dev.

`lastAccessedById` + `lastAccessedAt` update on every mutation
(`SaleSessionUpdateFinalizer.finalize` → `markTouched`), on
`SaleSessionHandler.acquireSession` (→ `markVisited`), and on successful
`saveDraft` (the persister bumps both inside `applySaleSaveResult` before saving).
`start` short-circuits and `listOpenSessions` are passive — they don't bump
the timestamp. This is what drives `SaleSessionSummaryDto.activeUser`.

---

## 5. Validation Split

| When           | Who                      | What                                                                                                                                                                                                                                                                                                                |
|----------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Every mutation | `SaleSessionValidator`   | Positive quantities, no duplicate products, valid adjustment line refs, adjustment reason + direction valid, positive payments, line + order discount ceilings                                                                                                                                                      |
| Save Draft     | `DraftSalePersister`     | If any payments in the input, future-date guard + fiscal period open; stock availability for requested quantities (`SaleValidator.guardSufficientStockForSale`); optimistic `@Version` check on `SaleEntity` (existing sales only). Note: drafts do NOT enforce a payments-within-payable-total ceiling.              |
| Confirm        | `ConfirmedSalePersister` | At least one line (enforced upstream by `SaleSessionValidator.guardNonEmptyLines`); future-date guard; fiscal period open; walk-in full coverage (enforced upstream); payments within payable total (enforced upstream); stock availability for requested quantities (`SaleValidator.guardSufficientStockForSale`); FIFO stock consumption; optimistic `@Version` check on `SaleEntity` (existing sales only). |

Structural validation runs on **every** mutation; commit-time guards run only
when the session is committed. The commit-time guards exist because the world
can change while the session sits in Redis (stock moves, fiscal period closes,
product gets deactivated).

---

## 6. Save Flow

### Save Draft (`POST /secured/sale-sessions/{sessionId}/draft`)
```
1. SaleSessionPersister.saveDraft
2. `guardMutable`, recompute totals, run full structural `validate`
   (`loadAndValidate`)
3. transform SaleSession → SaleSaveRequest (`SessionToSaveRequestMapper`)
4. DraftSalePersister.saveDraft:
     - if payments present: guard date-not-future + fiscal period open
       (`guardFiscalIfPayments`)
     - load-or-create SaleEntity at DRAFT (`loadDraftAtVersion` for existing —
       rejects non-DRAFT or version mismatch; new sales skip the version
       check and start at DRAFT)
     - sync lines (`SaleLineSync.sync` — insert new, update existing,
       delete missing)
     - clear reservations for this sale; if any persisted lines remain,
       guard stock availability (`guardSufficientStockForSale`), then re-reserve
     - SaleSaveFinalizer.finalize:
         - sync adjustments (`SaleAdjustmentSyncer.sync` — delete missing,
           update kept, insert new)
         - apply line + adjustment totals onto the sale (`SaleTotalsApplier`)
         - append new payments only (`SalePaymentAppender.appendNew`)
         - set `paymentStatus`, save sale
5. apply outcome to session (`SaleSessionPersister.applySaleSaveResult`):
   saleId, version, sync header (`referenceNumber`, `dateSold`, `soldById`
   from the persisted entity), flip transient→id where applicable, copy the
   resolved `paymentDate` onto each new session payment, bump
   `lastUpdatedAt` / `lastAccessedAt` / `lastAccessedById`
6. recompute totals, save session back
```

### Confirm (`POST /secured/sale-sessions/{sessionId}/confirm`)
```
1. SaleSessionPersister.confirm
2. `guardMutable`, recompute totals, run full structural `validate`
   (`loadAndValidate`)
3. `guardNonEmptyLines`, `guardWalkInFullyCovered`, `guardPaymentsWithinTotal`
4. transform SaleSession → SaleSaveRequest (`SessionToSaveRequestMapper`)
5. ConfirmedSalePersister.confirm:
     - guard date-not-future + fiscal period open (always, not gated on
       payments-present like the draft path)
     - load-or-create SaleEntity (`loadDraftAtVersion` for existing —
       rejects non-DRAFT or version mismatch; new sales start at CONFIRMED)
     - assign header, save sale
     - clear reservations for this sale (`clearBySale`) — before sync, same
       FK-ordering reason as the draft path
     - sync lines (`SaleLineSync.sync`)
     - guard stock availability (`SaleValidator.guardSufficientStockForSale`
       — this sale's own reservations are already cleared, so it checks
       against every other sale's live reservations)
     - flip status to CONFIRMED
     - SaleSaveFinalizer.finalize:
         - sync adjustments (`SaleAdjustmentSyncer.sync`)
         - apply totals (`SaleTotalsApplier`)
         - append new payments (`SalePaymentAppender.appendNew`)
         - set `paymentStatus`, save sale
     - run FIFO consumption via `SaleStockUpdater.consumeStock` (acquires the
       PRODUCT advisory lock again immediately before consuming, as a
       physical-stock backstop)
     - publish `SaleConfirmedEvent` via `SaleConfirmedHandlerForKafka`
6. apply outcome to session (`applySaleSaveResult` — new ids, header
   sync from persisted entity (`referenceNumber`, `dateSold`, `soldById`),
   `paymentDate` copied onto each new session payment, `lastUpdated*` /
   `lastAccessed*` bumped), build response, delete the session
```

### Void (`POST /secured/sale-sessions/{sessionId}/void`)
```
1. SaleSessionPersister.voidSale(sessionId, SaleSessionVoidDto(reason))
2. load session; reject if session has no persisted saleId
3. delegate to SaleUpdater.voidSale(SaleVoidCreateDto(saleId, reason)):
     - DRAFT  → DISCARDED (clears reservations)
     - CONFIRMED → VOIDED (restores stock, writes SaleVoidEntity,
       publishes SaleVoidedEvent; requires today's fiscal period open)
4. reflect resulting status on the session, build response, delete session
```

### CONFIRMED-session payments (no commit step)
When `originalStatus == CONFIRMED`, `POST /secured/sale-sessions/{sessionId}/payments`
forwards the payment to `SalePaymentService.recordPayment` synchronously and
folds the persisted id back into the session. There is no batched commit
endpoint — each add is durable when the handler returns.

The persisters live in `sale/api/` but operate on a `SaleSaveRequest` DTO —
they do not know about `SaleSession`. The translation `SaleSession →
SaleSaveRequest` happens through `SessionToSaveRequestMapper`, called from
`SaleSessionPersister`. This keeps the package boundary intact.

---

## 7. Optimistic locking

`SaleEntity.version` is a `@Version`-managed `Long`. When a session was loaded
from an existing draft, the session captures the version at load time
(`SaleSession.saleVersion`). On commit, `SaleDataFetcher.loadDraftAtVersion`
reloads the entity, asserts status is still DRAFT, and throws
`ObjectOptimisticLockingFailureException` upfront when the captured version
doesn't match — fail-fast at load time, rather than waiting for Hibernate's
flush-time conflict.

Mid-session operations that hit the sale row out-of-band must refresh
`SaleSession.saleVersion` so the next commit doesn't trip the check. Today
that means `SaleSessionPaymentHandler.add` (when `originalStatus == CONFIRMED`
and `SalePaymentService.recordPayment` runs) and `SaleSessionPaymentHandler.remove`
(when the payment was already persisted and routes through
`SalePaymentService.voidPayment`). Both bump `SaleEntity.version` via
`SaleUpdater.updatePaymentStatus`; the new version rides back on
`SalePaymentResponseDto.updatedSaleVersion` and the handlers copy it onto the
session before finalize.

---

## 8. Pitfalls

- **Persisted payments are not mutated at commit time.**
  `SalePaymentAppender.appendNew` only inserts payments with `existingId == null`;
  it does not update, delete, or validate the persisted payments already on
  the sale. The supported way to reverse a posted payment is `DELETE /payments`
  with a `voidReason`, which routes through `SaleSessionPaymentHandler.remove`
  → `SalePaymentService.voidPayment` and marks the payment row voided in
  place. The session keeps the payment with `voidedReason` set, so it stays
  visible to the cashier rather than disappearing from the list.
- **One session per sale.** `SaleSessionHandler.start({saleId})` returns the
  existing session instead of minting a new one; the `by-sale` Redis key is the
  source of truth.
- **Walk-in coverage is only enforced at confirm.** The mutation-time validator
  allows under-coverage so the cashier can build up payments incrementally.
- **Discount ceilings ARE enforced on every mutation.** Drafts can no longer
  go upside-down on discounts — this is a behavior change from the pre-session
  model.
- **`SaleSessionRedisStore` is the only class that touches Redis.** Nothing
  else in the app imports `RedisTemplate` / `StringRedisTemplate` — Redis
  access is fully encapsulated behind the `SaleSessionStore` interface.
- **Line `unitPrice` is a derived, unit-aware property.**
  `SaleSessionLine` stores `defaultSalePrice` (per base unit, captured on
  addition from `location_product.default_sale_price`), `baseUnitId`
  (snapshotted from `location_product.base_unit_id` on addition / on
  hydration), and `conversionRatio` (line unit → base unit, an exact
  rational — see `util/business/ConversionRatio`). `unitPrice`
  is a computed getter: `conversionRatio.applyTo(defaultSalePrice)`, so the
  per-line-unit price tracks the unit automatically —
  `SaleSessionLineHandler.applyLineChanges` only has to set
  `defaultSalePrice` / `conversionRatio`. Addition entries do not accept
  a `unitId`: new lines always start at the product's base unit
  (`unitId = baseUnitId`, `conversionRatio = ConversionRatio.IDENTITY`).
  Switching the line to a non-base unit is the job of an update entry in
  `applyLineChanges`. Update entries do **not** re-fetch `defaultSalePrice`;
  the value is held on the line for the life of the session.
  `DomainToSessionMapper` rehydrates `defaultSalePrice` for persisted
  lines via `unitPrice / conversionRatio.factor()`, and `baseUnitId` via a
  one-shot batch lookup (`LocationProductDataFetcher.getBaseUnitIds`)
  supplied by `SaleSessionLoader.loadFromSale`, so the DB price and base
  unit flow back in unchanged on session reload. The outbound
  `SaleSessionLineResponse` still exposes a single derived
  `conversionFactor` decimal — the exact ratio never needs to leave the
  server.

---

## 9. Price Override

A cashier sets a custom price on a line by supplying `unitPriceOverride` in a
`SaleSessionLineUpdateDto`. The client always sends this field (non-nullable); on line add
the response always carries `unitPriceOverride == unitPrice`. The server owns the adjustment
model — the client never knows about `PRICE_OVERRIDE` adjustments, it only sees and echoes
`unitPriceOverride`.

- `PriceOverrideReconciler` (called by `SaleSessionLineHandler.applyLineChanges`)
  computes `unitDiff = unitPrice − unitPriceOverride` and upserts a
  `PRICE_OVERRIDE` adjustment with `value = |unitDiff|` and direction
  `DISCOUNT` / `SURCHARGE` per sign. Storage is **per-unit** — the
  `AdjustmentAmountCalculator` scales by line `quantity` at compute time,
  so qty changes need no recalc. If the diff is zero the override is
  removed. **Unit change clears the override unless a new price is supplied:**
  when a line's unit changes and the client echoes back the existing effective
  price, `PriceOverrideReconciler` drops the `PRICE_OVERRIDE`; when the client
  supplies a different `unitPriceOverride`, the reconciler treats it as a new
  explicit override computed against the new unit's `unitPrice`. Non-price-override
  line adjustments are always cleared on a unit change: `SaleSessionLineHandler.applyLineChanges`
  filters any remaining line-level adjustment whose `relatedSaleLineIdentity` targets
  a unit-changed line. **Echo detection:** the reconciler always derives the previous
  effective price — `unitPrice ± storedValue` when an override exists, otherwise
  `unitPrice` — and treats a submitted value that matches as a no-op. This keeps
  all business logic server-side; the client echoes the full line state without
  consequence.
- `unitPriceOverride == 0` is allowed (cashier giveaway). Negative is
  rejected by `SaleSessionValidator.guardNonNegativeEffectivePrices`.
- The `PRICE_OVERRIDE` adjustment is stored in Redis just like any other
  adjustment and persisted to the DB at `saveDraft` / `confirm` via
  `SaleAdjustmentSyncer`.
- `unitPriceOverride` on `SaleSessionLineResponse` is always populated:
  `SaleSessionAssembler.buildLineMappingContext` derives it by reversing the
  stored adjustment (`unitPrice ± value`) when an override exists, and falls
  back to `unitPrice` otherwise — never stored in Redis or the DB directly.
- Mutual exclusivity (a line cannot carry both a `PRICE_OVERRIDE` and any
  other adjustment) is enforced structurally inside `validate()` via
  `guardLineOverrideExclusivity`, so it holds regardless of which entry
  point produced the state (line update, adjustment add, load-from-DB).

## 10. Where Things Live

| Topic                           | Class(es)                                                                                                                                                                                                                          |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Session model                   | `SaleSession`, `SaleSessionHeader`, `SaleSessionLine`, `SaleSessionAdjustment`, `SaleSessionPayment`, `SaleSessionTotals`                                                                                                          |
| Identity                        | `SessionIdentity`                                                                                                                                                                                                                  |
| Storage                         | `SaleSessionStore`, `SaleSessionRedisStore`, `SaleSessionInMemoryStore`                                                                                                                                                            |
| Validation                      | `SaleSessionValidator`                                                                                                                                                                                                             |
| Price override reconciliation   | `PriceOverrideReconciler`                                                                                                                                                                                                          |
| Totals                          | `SaleSessionTotalsCalculator`                                                                                                                                                                                                      |
| Load / mint                     | `SaleSessionLoader`, `DomainToSessionMapper`                                                                                                                                                                                       |
| Build response/summary          | `SaleSessionAssembler`, `SessionToResponseMapper`                                                                                                                                                                                  |
| Post-mutation pipeline          | `SaleSessionUpdateFinalizer`                                                                                                                                                                                                       |
| Session orchestration           | `SaleSessionHandler`, `SaleSession{Header,Line,Adjustment,Payment}Handler`, `SaleSessionPersister`                                                                                                                                 |
| Save primitives (in sale/api)   | `SaleSaveRequest`, `SaleSaveResult`, `DraftSalePersister`, `ConfirmedSalePersister`                                                                                                                                                |
| Save sub-pieces                 | `SaleLineSync` (sale internal), `SaleSaveFinalizer` (sale internal), `SaleAdjustmentSyncer` (sale_adjustment/api), `SalePaymentAppender` (sale_payment/api, delegates to `SalePaymentWriter`), `SaleTotalsApplier` (sale internal) |
| Session→SaleSaveRequest mapping | `SessionToSaveRequestMapper`                                                                                                                                                                                                       |
| REST entry point                | `SaleSessionEndpoint` at `/secured/sale-sessions`                                                                                                                                                                                  |
