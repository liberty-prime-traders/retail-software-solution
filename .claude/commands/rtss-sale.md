# RTSS — Sale & Sale Session Domain

Read this before touching anything in `sale/`, `sale_session/`, `sale_adjustment/`, or `sale_payment/`.
Then read the relevant README before editing and update it after.

- `sale/README.md` — sale lifecycle, stock, taxes, payments, pitfalls
- `sale_session/README.md` — session lifecycle, storage, validation split, price override

---

## Package boundaries

```
sale_session/ → may call sale/api/, sale_adjustment/api/, sale_payment/api/, stock/api/
sale/         → knows nothing about sale_session (convention, not test-enforced)
```

`ArchitectureTest` enforces the `.api` boundary. Never reach into non-`api` classes from outside a domain.

## sale/ public surface (api/ only)

| Class                    | Purpose                                                          |
|--------------------------|------------------------------------------------------------------|
| `DraftSalePersister`     | Persist at DRAFT — called by sale_session only, no REST endpoint |
| `ConfirmedSalePersister` | Persist at CONFIRMED — FIFO + Kafka                              |
| `SaleUpdater`            | Void a sale; update payment status; update notes                 |
| `SaleDataFetcher`        | All reads: recent list, header/line snapshots, locking lookups   |

## sale_session/ public surface (api/ only)

`SaleSessionHandler`, `SaleSessionHeaderHandler`, `SaleSessionLineHandler`, `SaleSessionAdjustmentHandler`, `SaleSessionPaymentHandler`, `SaleSessionPersister`

## Sale lifecycle

```
create → DRAFT → CONFIRMED → VOIDED
              ↓
          DISCARDED  (void on draft — no stock movement, no Kafka event)
```

- VOIDED and DISCARDED are terminal.
- Creating or confirming a sale is **only** possible through a session.
- `SaleStatus` persisted as short code (`DFT`, `CFM`, `VD`, `DSC`) via `SaleStatusConverter`. Never `@Enumerated`.

## Session lifecycle

- Session lives in Redis (TTL 2h). Every mutation: load → apply → recompute totals → validate → save.
- `saveDraft` flushes to DB and keeps the session alive. `confirm` and `abandon` terminate it.
- One session per sale — `start({saleId})` returns the existing session if one is open.
- `originalStatus` at load time gates what mutations are allowed (CONFIRMED sessions reject line/header edits).

## Key invariants to preserve

- **Reservations**: always `clearBySale` before re-issuing at commit. Never diff-based reservation logic.
- **Kafka events**: always via `ApplicationEventPublisher` so they bind to the transaction commit.
- **`grandTotal`** is null at commit time — async Kafka tax processor fills it. Use `payableTotal()` if you need a real number in the same transaction.
- **`dateSold`** comparisons use org timezone — always `DateTimes.Local.Now.organization()`, never `LocalDate.now()` / `ZoneOffset.UTC`.
- **Walk-in** (`SystemContact.WALK_IN.id`) must be fully paid at confirm; no DB lookup required for walk-in existence check.
- **Discount ceilings** enforced on every session mutation — line-level discounts cannot exceed the line total; order-level cannot exceed subtotal minus line-level discounts.
- **Price override** is a `PRICE_OVERRIDE` adjustment managed by `PriceOverrideReconciler` — the client only sees/echoes `effectivePrice`, never the underlying adjustment.

## Validation ordering (preserve when adding guards)

1. Pure DTO / session-state (free)
2. Cheap status guards (single FK lookup)
3. Single-row lookups (contact, fiscal period)
4. Bulk fetches (product summaries, unit graph, balances)
5. Stock guards (advisory lock)
6. Persist
7. Publish Kafka events (last)

## Common pitfalls (from README §14 / §8)

- Never compute `grandTotal` synchronously — use `payableTotal()`.
- Never add a guard after a bulk fetch — move it earlier.
- Never bypass the session for line/adjustment edits — no REST surface exists to mutate a draft directly.
- Never publish Kafka events outside the sale transaction.
- Kafka publishers `!!` on `sale.id` and `sale.dateSold` — ensure those are set before any new publish path.
