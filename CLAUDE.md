# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Standards

Read `.claude/instructions.md` on every session start — it is the authoritative coding standards doc.

Quick rules:
- YAML: two-space indentation.
- Imports: never star imports; FQNs only on name conflicts.
- Comments: gotchas only — never explain what the code already says.
- Variable and param names: verbose, never abbreviated; param name must match its type (`saleSaveRequest: SaleSaveRequest`, not `input: SaleSaveRequest`).
- Endpoints are thin delegators — mapping/assembler logic lives in services, never in the endpoint body.

## Commands

```bash
# Start infrastructure (Postgres, Kafka, Redis)
cd docker && ./dev.sh          # up
cd docker && ./dev.sh down     # down

# Build
./gradlew build

# Unit tests (H2, no Docker needed)
./gradlew test

# Run a single test class
./gradlew test --tests "me.ezra_home.retail_software_solution.SomeTest"

# Cucumber integration tests (Testcontainers — needs Docker)
./cucumber.sh                  # regression (default)
./cucumber.sh smoke
./cucumber.sh kafka-producer
./cucumber.sh kafka-consumer
```

## Architecture

The app is a Spring Boot + Kotlin monolith backed by **three independent datasources** — `platform`, `organization`, and `location` — each with its own Liquibase changelog and connection pool (configured in `application.yml`). Schemas are per-organization (`org-a`, `org-b`, …) and per-location (`loc-1`, `loc-2`, …); the location datasource sets the search path dynamically per request via `@TransactionalOnLocationSchema`.

### Layer structure

```
platform/       — cross-org metadata: users, table registry, db versioning, jurisdiction/tax setup
organizations/  — org-scoped data: products, contacts, payment methods, fiscal periods, adjustment reasons
locations/      — location-scoped operations: sales, purchases, inventory, stock
```

Each business domain lives under `*/business/<domain>/`. Anything not under `api/` is internal to that domain. **`ArchitectureTest` enforces this** — cross-domain access to non-`api` classes will fail the test suite. When cross-domain communication is needed, import from `<domain>/api/` only.

### Key locations domains

| Domain | What it owns |
|---|---|
| `sale/` + `sale_session/` | Full sale lifecycle; session is the only path to create/confirm a sale |
| `purchase/` + `delivery/` + `supplier_payment/` | Purchase orders, deliveries (stock-in), supplier payments |
| `stock/` | Stock balances, reservations, FIFO movement |
| `location_product/` | Per-location product catalog and pricing |
| `sale_adjustment/` | Discounts and surcharges (shared by sale and sale_session) |
| `tax_entry/` | Async tax finalization records (written by Kafka consumers) |

Each domain with non-obvious rules has a `README.md` — read it before touching the package, update it after.

### Messaging

Kafka events are published via `ApplicationEventPublisher` and bind to the enclosing transaction commit (a rolled-back transaction never reaches the broker). Every processor implements `shouldProcess` backed by a unique DB constraint on the natural key so retries are idempotent.

### Utilities to always use

- `DateTimes` — all `LocalDate` / `OffsetDateTime` logic; never `LocalDate.now()` or `ZoneOffset.UTC`
- `Decimals` — monetary arithmetic (scale 4, half-up)
- `StringUtils` — blank-to-null and other string helpers

### Database migrations

Liquibase changelogs live under `src/main/resources/db/changelog/{platform,organizations,locations}/`. Within each layer the load order is: `tables/ → sequences/ → foreign_keys/ → triggers/ → indexes/ → audit/ → version-entries/ → registry-entries/` (the last two are platform-only). All new tables must be registered in `registry-entries/` and in the `TableNames` constants class.

## Agent discipline

Before starting a multi-step task: write a plan to `.claude/plan.txt` and update it as you go. Read at most 3 files before making a first edit, then adjust. Checkpoint every 5 tool calls — state your hypothesis and decide whether to continue or back out.
