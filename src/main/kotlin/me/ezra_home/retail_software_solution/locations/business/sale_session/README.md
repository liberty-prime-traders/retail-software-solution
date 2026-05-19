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

| Class                          | Purpose                                               |
|--------------------------------|-------------------------------------------------------|
| `SaleSessionHandler`           | Start, abandon, list, get a session                   |
| `SaleSessionHeaderHandler`     | Patch contactId, soldById, dateSold, notes            |
| `SaleSessionLineHandler`       | Add, update, remove session lines                     |
| `SaleSessionAdjustmentHandler` | Add, remove session adjustments                       |
| `SaleSessionPaymentHandler`    | Add, remove session payments                          |
| `SaleSessionPersister`         | `saveDraft`, `confirm` — server-side commit pipelines |

Internal collaborators (do not call from outside `sale_session`):

- `SaleSessionStore` — storage interface; `SaleSessionRedisStore`
  (`redis-active` profile) or `SaleSessionInMemoryStore` (default —
  `!redis-active`)
- `SaleSessionLoader` — `loadFromSale` rehydrates an existing `SaleEntity` into
  the session shape; `newSession` mints a fresh empty session
- `SaleSessionAssembler` — orchestrates response/summary DTO assembly;
  delegates per-bean translation to `SaleSessionMapper` (MapStruct)
- `SaleSessionValidator` — runs structural validation on every mutation
- `SaleSessionTotalsCalculator` — recomputes totals after every mutation
- `SaleSessionUpdateFinalizer` — touch → recompute → validate → save → buildResponse
  pipeline invoked by every mutation handler

Architectural boundary:

```
sale_session/ → may call sale/api/, sale_adjustment/api/, sale_payment/api/
sale/         → knows nothing about sale_session (convention)
```

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
POST {id}/draft     POST {id}/confirm    POST {id}/payments     DELETE {id}
DraftSalePersister  ConfirmedSalePersister (CONFIRMED branch:    (abandon)
(persists at        (persists at          SalePaymentService    drops session,
 DRAFT status;       CONFIRMED status;    .recordPayment        no DB writes
 session lives on)   then deletes         per add call)
                     session)
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

| `originalStatus` | Lines / adjustments / header | Add payment                      | Remove payment                                                       | Save path              |
|------------------|------------------------------|----------------------------------|----------------------------------------------------------------------|------------------------|
| `DRAFT`          | ✅ allowed                    | ✅ allowed (buffered)             | ✅ transient: dropped; persisted: voided (row stays, marked)          | `saveDraft`, `confirm` |
| `CONFIRMED`      | ❌ rejected                   | ✅ allowed (recorded immediately) | ✅ persisted only: voided (row stays, marked)                         | none (no commit step)  |
| `VOIDED`         | ❌ rejected                   | ❌ rejected                       | ❌ transient: rejected; persisted: rejected (`guardSaleNotVoided`)    | none (view-only)       |
| `DISCARDED`      | ❌ rejected                   | ❌ rejected                       | transient: ❌ rejected; persisted: ⚠️ leaks through (see prose below) | none (view-only)       |

`SaleSessionValidator.guardMutable` enforces the line/adjustment/header rules;
`SaleSessionValidator.canAddPayments` gates add-payment and
`SaleSessionValidator.canDiscardPayments` gates remove-payment. The commit
handler calls `guardMutable` before either commit path.

The single `DELETE /payments` endpoint handles both removal cases: when the
payment's `SessionIdentity` is `transient` the handler runs
`SaleSessionValidator.canDiscardPayments` (which permits only DRAFT) and drops it; when it's `persisted` the handler
forwards to `SalePaymentService.voidPayment` (a `voidReason` is required in
that case), which in turn calls `SalePaymentValidator.guardSaleNotVoided`.
That keeps the frontend wire-protocol uniform — server decides which side to
take based on the payment's identity. Note `guardSaleNotVoided` only checks
the VOIDED status; DISCARDED is not blocked there, so removing a persisted
payment from a DISCARDED-loaded session is not currently rejected on the
service side.

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
      `unitPrice`, `conversionFactor` onto the existing entity. In practice
      `locationProductId` and `unitPrice` never change (no session handler
      mutates them), so only quantity / unit / factor effectively differ.
    - **adjustments** and **payments** are kept untouched — there are no
      "update" handlers for these in-session, so existing rows can only be
      retained or removed/voided, never mutated
  - `transientId != null` → insert a new row, flip `transientId` to `id` after
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
| Save Draft     | `DraftSalePersister`     | If any payments in the input, future-date guard + fiscal period open; stock availability for requested quantities (`SaleValidator.guardStockForDraftUpdates`); optimistic `@Version` check on `SaleEntity` (existing sales only). Note: drafts do NOT enforce a payments-within-payable-total ceiling.              |
| Confirm        | `ConfirmedSalePersister` | At least one line (enforced upstream by `SaleSessionValidator.guardNonEmptyLines`); future-date guard; fiscal period open; walk-in full coverage (enforced upstream); payments within payable total (enforced upstream); FIFO stock consumption; optimistic `@Version` check on `SaleEntity` (existing sales only). |

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
3. transform SaleSession → SaleSaveRequest (`SaleSaveRequestMapper`)
4. DraftSalePersister.saveDraft:
     - if payments present: guard date-not-future + fiscal period open
       (`guardFiscalIfPayments`)
     - load-or-create SaleEntity at DRAFT (`loadDraftAtVersion` for existing —
       rejects non-DRAFT or version mismatch; new sales skip the version
       check and start at DRAFT)
     - sync lines (`SaleLineSync.sync` — insert new, update existing,
       delete missing)
     - clear reservations for this sale; if any persisted lines remain,
       guard stock availability (`guardStockForDraftUpdates`), then re-reserve
     - SaleSaveFinalizer.finalize:
         - sync adjustments (`SaleAdjustmentSyncer.sync` — delete missing,
           update kept, insert new)
         - apply line + adjustment totals onto the sale (`SaleTotalsApplier`)
         - append new payments only (`SalePaymentAppender.appendNew`)
         - set `paymentStatus`, save sale
5. apply outcome to session (`SaleSessionPersister.applySaleSaveResult`):
   saleId, version, flip transient→id where applicable, bump
   `lastUpdatedAt` / `lastAccessedAt` / `lastAccessedById`
6. recompute totals, save session back
```

### Confirm (`POST /secured/sale-sessions/{sessionId}/confirm`)
```
1. SaleSessionPersister.confirm
2. `guardMutable`, recompute totals, run full structural `validate`
   (`loadAndValidate`)
3. `guardNonEmptyLines`, `guardWalkInFullyCovered`, `guardPaymentsWithinTotal`
4. transform SaleSession → SaleSaveRequest (`SaleSaveRequestMapper`)
5. ConfirmedSalePersister.confirm:
     - guard date-not-future + fiscal period open (always, not gated on
       payments-present like the draft path)
     - load-or-create SaleEntity (`loadDraftAtVersion` for existing —
       rejects non-DRAFT or version mismatch; new sales start at CONFIRMED)
     - assign header, save sale
     - sync lines (`SaleLineSync.sync`)
     - acquire PRODUCT advisory lock over the persisted lines'
       location-product ids, clear reservations
     - flip status to CONFIRMED
     - SaleSaveFinalizer.finalize:
         - sync adjustments (`SaleAdjustmentSyncer.sync`)
         - apply totals (`SaleTotalsApplier`)
         - append new payments (`SalePaymentAppender.appendNew`)
         - set `paymentStatus`, save sale
     - run FIFO consumption via `SaleStockUpdater.consumeStock`
     - publish `SaleConfirmedEvent` via `SaleConfirmedHandlerForKafka`
6. apply outcome to session (`applySaleSaveResult` — new ids,
   `lastUpdated*` / `lastAccessed*` bumped), build response, delete the
   session
```

### CONFIRMED-session payments (no commit step)
When `originalStatus == CONFIRMED`, `POST /secured/sale-sessions/{sessionId}/payments`
forwards the payment to `SalePaymentService.recordPayment` synchronously and
folds the persisted id back into the session. There is no batched commit
endpoint — each add is durable when the handler returns.

The persisters live in `sale/api/` but operate on a `SaleSaveRequest` DTO —
they do not know about `SaleSession`. The translation `SaleSession →
SaleSaveRequest` happens through `SaleSaveRequestMapper`, called from
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
- **Line `unitPrice` is frozen for the life of the session** — captured at
  add time for transient lines, and at load time (from the DB snapshot) for
  persisted lines. `SaleSessionLineHandler.updateLine` does not re-fetch
  `unitPrice`, so changing the product price on the location_product table
  has no effect on already-added session lines.

---

## 9. Where Things Live

| Topic                           | Class(es)                                                                                                                                                                                        |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Session model                   | `SaleSession`, `SaleSessionHeader`, `SaleSessionLine`, `SaleSessionAdjustment`, `SaleSessionPayment`, `SaleSessionTotals`                                                                        |
| Identity                        | `SessionIdentity`                                                                                                                                                                                |
| Storage                         | `SaleSessionStore`, `SaleSessionRedisStore`, `SaleSessionInMemoryStore`                                                                                                                          |
| Validation                      | `SaleSessionValidator`                                                                                                                                                                           |
| Totals                          | `SaleSessionTotalsCalculator`                                                                                                                                                                    |
| Load / mint                     | `SaleSessionLoader`                                                                                                                                                                              |
| Build response/summary          | `SaleSessionAssembler`, `SaleSessionMapper`                                                                                                                                                      |
| Post-mutation pipeline          | `SaleSessionUpdateFinalizer`                                                                                                                                                                     |
| Session orchestration           | `SaleSessionHandler`, `SaleSession{Header,Line,Adjustment,Payment}Handler`, `SaleSessionPersister`                                                                                               |
| Save primitives (in sale/api)   | `SaleSaveRequest`, `SaleSaveResult`, `DraftSalePersister`, `ConfirmedSalePersister`                                                                                                              |
| Save sub-pieces                 | `SaleLineSync` (sale internal), `SaleSaveFinalizer` (sale internal), `SaleAdjustmentSyncer` (sale_adjustment/api), `SalePaymentAppender` (sale_payment/api), `SaleTotalsApplier` (sale internal) |
| Session→SaleSaveRequest mapping | `SaleSaveRequestMapper`                                                                                                                                                                          |
| REST entry point                | `SaleSessionEndpoint` at `/secured/sale-sessions`                                                                                                                                                |
