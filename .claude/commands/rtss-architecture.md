# RTSS — Architecture Patterns

Read this when adding a new flow, cross-layer call, Kafka processor, or any new domain scaffolding.

---

## Layer structure

```
platform/       — cross-org metadata (users, table registry, jurisdiction/tax setup, db versioning)
organizations/  — org-scoped data (products, contacts, payment methods, fiscal periods)
locations/      — location-scoped operations (sales, purchases, inventory, stock)
```

Three independent datasources — `platform`, `organization`, `location` — each with its own connection pool and Liquibase changelog. The location datasource sets the schema search path per request via `@TransactionalOnLocationSchema`.

## Cross-layer rules

- Communicate across domains through `<domain>/api/` only.
- `ArchitectureTest.domainsShouldBeSelfContained` enforces this — it will fail if non-`api` classes are imported from outside the domain.
- Never import from another domain's internal classes directly, even "just for a read".
- Shared utilities (`util/`, `exceptions/`, etc.) are exempt from the boundary rule.

## Insert flow

```
InsertDto → Entity (via mapper) → save → DomainDto (released to service layer)
```

- The `DomainDto` is what crosses package lines on the read path. It has correct types and no unnecessary nullables.

## Update flow

```
UpdateDto.applyTo(existing: XxxDomainDto): XxxDomainDto
```

- Only updatable fields are on `UpdateDto`. Most are `Optional<TYPE>?`:
  - `null` = not provided, leave it alone
  - `Optional.empty()` = explicitly clear
  - `Optional.of(x)` = set to x
- Generated fields (`id`, `referenceNumber`, `createdOn`, `createdById`) carry through automatically via `copy()`.
- Do NOT use `@MappingTarget` mappers for partial updates.

## MapStruct

- Use `@Context` parameters to pass additional data needed for mapping.
- This avoids O(n) queries inside mappers when mapping collections.

## DateTimes — always use this utility

```kotlin
DateTimes.Local.Now.organization()       // LocalDate at org timezone
DateTimes.Offset.Now.organization()      // OffsetDateTime at org timezone
DateTimes.Local.atOrganizationZone(odt)  // OffsetDateTime → LocalDate at org timezone
```

Never `LocalDate.now()`. Never `ZoneOffset.UTC` for org-scoped date logic.

## Kafka processors — required pattern for every new processor

1. Implement `shouldProcess` as a fast existence check on the natural key of what the processor writes.
2. Back that check with a **unique DB constraint** on the natural key — retries must be idempotent at the DB layer too.
3. Wrap `handle` in `@TransactionalOnLocationSchema`; `shouldProcess` may be `readOnly = true`.
4. Publish events via `ApplicationEventPublisher` — events bind to transaction commit and are never emitted for rolled-back transactions.
5. Implement `EventReissueHandler` so administrative replays rebuild the same payload from the database.

## Advisory locking pattern

- `EntityAdvisoryLock.acquire(LockNamespaces.X, ids)` — take the lock before any mutation on that entity.
- Lock helpers that are `Propagation.MANDATORY` must join an existing writable transaction — call them from inside a `@Transactional` method.
- Never acquire locks manually outside the domain's designated helper methods.

## Agent discipline

- Write plan to `.claude/plan.txt` before starting; update as you go.
- Read ≤3 files before making a first edit.
- Checkpoint every 5 tool calls: state hypothesis, decide continue or back out.
- If stuck, stop and reassess — don't loop.
