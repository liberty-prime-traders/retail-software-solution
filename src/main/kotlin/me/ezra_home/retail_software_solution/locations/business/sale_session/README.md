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

| Class                          | Purpose                                                          |
|--------------------------------|------------------------------------------------------------------|
| `SaleSessionHandler`           | Start, abandon, list, get a session                              |
| `SaleSessionHeaderHandler`     | Patch contactId, soldById, dateSold, notes                       |
| `SaleSessionLineHandler`       | Add, update, remove session lines                                |
| `SaleSessionAdjustmentHandler` | Add, remove session adjustments                                  |
| `SaleSessionPaymentHandler`    | Add, remove session payments                                     |
| `SaleSessionCommitHandler`     | Save draft / confirm — both run through the session              |

Internal collaborators (do not call from outside `sale_session`):

- `SaleSessionStore` — Redis facade; `SaleSessionRedisStore` (default profile)
  or `SaleSessionInMemoryStore` (`sessionInMemory` profile)
- `SaleSessionLoader` — converts an existing `SaleEntity` into the session shape
- `SaleSessionAssembler` — builds response/summary DTOs from a session
- `SaleSessionValidator` — runs structural validation on every mutation
- `SaleSessionTotalsCalculator` — recomputes totals after every mutation

Architectural boundary (enforced by `ArchitectureTest`):

```
sale_session/ → may call sale/api/, sale_adjustment/api/, sale_payment/api/
sale/         → knows nothing about sale_session
```

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
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
   POST /sale-sessions/{id}/draft   POST /sale-sessions/{id}/confirm
   SaleDraftCommitter               SaleConfirmCommitter
   (sale ends DRAFT)                (sale ends CONFIRMED, session deleted)
```

`SaleSessionHandler.start` short-circuits when an active session already exists
for the supplied `saleId` — returns the existing one (one-session-per-sale
guarantee).

Only `DRAFT` sales can be loaded into a new session. Already-confirmed sales
and voided/discarded sales are rejected at start time.

---

## 3. SessionIdentity

Every row in a session (line, adjustment, payment) carries a `SessionIdentity`:

```
SessionIdentity
  ├── id: UUID?          — set when the row exists in DB
  └── transientId: UUID? — set when the row only exists in Redis
```

- Exactly one is non-null — enforced by the constructor.
- Frontend echoes it back as-is when targeting a row.
- On commit:
  - `id != null` → update existing DB row
  - `transientId != null` → insert a new row, flip `transientId` to `id` after

`SessionIdentity.key()` returns whichever UUID is set; use it as a lookup key
inside the session.

---

## 4. Storage

Two Redis keys per session and one index:

```
sale-session:{sessionId}      → JSON-serialized SaleSession (TTL configurable)
sale-session:by-sale:{saleId} → sessionId string (set only when session.saleId != null)
sale-session:open             → Set<sessionId> driving `listOpen()`
```

`SaleSessionRedisStore` keeps all three in lockstep on every save / delete.
`SaleSessionInMemoryStore` is API-equivalent for tests / local dev.

`lastAccessedById` + `lastAccessedAt` update on every read AND every write so
`SaleSessionSummaryDto.activeUser` reflects current presence.

---

## 5. Validation Split

| When             | Who                          | What                                                                                                                                                       |
|------------------|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Every mutation   | `SaleSessionValidator`       | Positive quantities, no duplicate products, valid adjustment line refs, adjustment reason + direction valid, positive payments, line + order discount ceilings |
| Save Draft       | `SaleDraftCommitter`         | Products still active, fiscal period open (only if payments present), optimistic `@Version` check on `SaleEntity`                                          |
| Confirm          | `SaleConfirmCommitter`       | At least one line, products still active, future-date guard, fiscal period open, walk-in full coverage, payments within payable total, FIFO stock consumption |

Structural validation runs on **every** mutation; commit-time guards run only
when the session is committed. The commit-time guards exist because the world
can change while the session sits in Redis (stock moves, fiscal period closes,
product gets deactivated).

---

## 6. Commit Flow

### Save Draft (`POST /sale-sessions/{id}/draft`)
```
1. SaleSessionCommitHandler.saveDraft
2. validate (last structural pass)
3. transform SaleSession → SaleCommitInput
4. SaleDraftCommitter.saveDraft:
     - guard products active
     - guard fiscal if payments
     - load-or-create SaleEntity at DRAFT
     - sync lines (insert new, update existing, delete missing)
     - clear reservations for this sale, re-reserve current lines
     - sync adjustments (delete missing, insert new)
     - append new payments
     - recompute totals, save
5. update session: saleId, version, flip transient→id where applicable
6. save session back
```

### Confirm (`POST /sale-sessions/{id}/confirm`)
```
1. SaleSessionCommitHandler.confirm
2. validate (last structural pass)
3. guard walk-in full coverage, payments within payable total
4. transform SaleSession → SaleCommitInput
5. SaleConfirmCommitter.confirm:
     - guard ≥1 line, products active, date + fiscal period
     - load-or-create SaleEntity (becomes CONFIRMED at end)
     - sync lines
     - acquire PRODUCT advisory lock, clear reservations
     - sync adjustments
     - append payments (walk-in coverage already guarded above)
     - run FIFO consumption via SaleStockUpdater
     - publish SaleConfirmedEvent
6. update session with new ids and delete the session
```

The committers live in `sale/api/` but operate on a `SaleCommitInput` DTO —
they do not know about `SaleSession`. The translation `SaleSession →
SaleCommitInput` happens inside `SaleSessionCommitHandler`. This keeps the
package boundary intact.

---

## 7. Optimistic locking

`SaleEntity.version` is a `@Version`-managed `Long`. When a session was loaded
from an existing draft, the session captures the version at load time
(`SaleSession.saleVersion`). On commit, the committer reloads the entity and
rejects when the captured version no longer matches — surfacing a clean
"someone else changed this sale" error rather than letting Hibernate's
optimistic lock fire later.

---

## 8. Pitfalls

- **Persisted payments cannot be removed from a session.** `SalePaymentCommitter.ensureRemovalsRejected`
  refuses commits that drop a previously-persisted payment — voids are the only
  legitimate way to reverse a posted payment.
- **One session per sale.** `SaleSessionHandler.start({saleId})` returns the
  existing session instead of minting a new one; the `by-sale` Redis key is the
  source of truth.
- **Walk-in coverage is only enforced at confirm.** The mutation-time validator
  allows under-coverage so the cashier can build up payments incrementally.
- **Discount ceilings ARE enforced on every mutation.** Drafts can no longer
  go upside-down on discounts — this is a behavior change from the pre-session
  model.
- **`SaleSessionStore` is the only class that touches Redis.** Nothing else in
  the app imports Lettuce / `RedisTemplate`.
- **Existing line `unitPrice` is captured at add time** and frozen for the
  life of the session. Changing the product price on the location_product table
  has no effect on already-added session lines.

---

## 9. Where Things Live

| Topic                                | Class(es)                                                          |
|--------------------------------------|--------------------------------------------------------------------|
| Session model                        | `SaleSession`, `SaleSessionHeader`, `SaleSessionLine`, `SaleSessionAdjustment`, `SaleSessionPayment`, `SaleSessionTotals` |
| Identity                             | `SessionIdentity`                                                  |
| Storage                              | `SaleSessionStore`, `SaleSessionRedisStore`, `SaleSessionInMemoryStore` |
| Validation                           | `SaleSessionValidator`                                             |
| Totals                               | `SaleSessionTotalsCalculator`                                      |
| Load / mint                          | `SaleSessionLoader`                                                |
| Build response/summary               | `SaleSessionAssembler`                                             |
| Session orchestration                | `SaleSessionHandler`, `SaleSession{Header,Line,Adjustment,Payment}Handler`, `SaleSessionCommitHandler` |
| Commit primitives (in sale/api)      | `SaleCommitInput`, `SaleDraftCommitter`, `SaleConfirmCommitter`    |
| Commit sub-pieces                    | `SaleCommitLineSync` (sale internal), `SaleAdjustmentCommitter` (sale_adjustment/api), `SalePaymentCommitter` (sale_payment/api), `SaleTotalsApplier` (sale internal) |
| REST entry point                     | `SaleSessionEndpoint` at `/secured/sale-sessions`                  |
