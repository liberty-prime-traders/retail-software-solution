# RTSS Skills Plan

Skills to load project context fast on future tasks. Each is a focused prompt file
under `.claude/commands/` that you invoke as a slash command.

---

## Skill 1 — `rtss-conventions`

**Trigger**: any task touching naming, comments, imports, formatting, or code style.

**Covers**
- Verbose variable/param names (never abbreviate; param name matches type)
- No star imports; use import statements; FQNs only on conflict
- Comments only for gotchas, never explanatory
- YAML two-space indentation
- No data manipulation in endpoints — thin delegators only
- Endpoint bodies delegate; mapping/assembler logic lives in services

**Source**: `CLAUDE.md`, `instructions.md` Naming & Formatting section, memory files:
`feedback_no_abbreviations`, `feedback_parameter_naming`, `feedback_no_manipulation_in_endpoints`

---

## Skill 2 — Per-domain skills (one skill per package cluster)

Rather than one monolithic domain skill, each significant domain gets its own skill.
A domain earns a skill when it has a `README.md` that encodes non-obvious rules —
that README is the source of truth, and the skill is the fast-load prompt for it.

### When to create a new domain skill

Build a skill for a package when **any two** of these are true:
- The package has a `README.md` with lifecycle / boundary rules
- It has its own public `api/` sub-package
- You've had to re-read the README more than once in the same session

### Current domain skills

#### `rtss-sale`

**Trigger**: any task in `locations/business/sale/` or `locations/business/sale_session/`

**Covers**
- Sale lifecycle: `DRAFT → CONFIRMED → VOIDED / DISCARDED`
- Sale session is the **only** path to create or confirm a sale
- Public surface of `sale/` (DraftSalePersister, ConfirmedSalePersister, SaleUpdater, SaleDataFetcher)
- Public surface of `sale_session/` (handlers, SaleSessionPersister)
- Internal classes that must not be called from outside the package
- Boundary: `sale_session → sale/api/`; `sale/` knows nothing about `sale_session/`

**Source**: `sale/README.md`, `sale_session/README.md`,
memory: `project_sale_readme`, `feedback_readmes_must_match`, `feedback_verifying_long_doctrine_docs`

---

#### `rtss-purchase`

**Trigger**: any task in `locations/business/purchase/`

**Covers**
- Purchase lifecycle and public `api/` surface
- Delivery and supplier payment rules
- Serializable transaction scope quirks
- PurchaseUpdater REST exposure

**Source**: `purchase/README.md`, memory: `project_purchase_readme`

---

#### `rtss-fiscal`

**Trigger**: any task in `organizations/business/fiscal_period/`

**Covers**
- Fiscal period lifecycle and constraints
- Rules encoded in `fiscal_period/README.md`

**Source**: `fiscal_period/README.md` (build this skill once the README matures)

---

### Convention for future domains

When adding a new skill for a domain:
1. Name it `rtss-<package-name>` (snake-case package name, drop the path prefix)
2. Trigger on the package path
3. Source from that package's `README.md` first, then memory
4. Add an entry here under "Current domain skills"

---

## Skill 3 — `rtss-db`

**Trigger**: any task touching Liquibase changesets, entities, repositories, constraints, or audit tables.

**Covers**
- Changeset reuse for drops (edit in-place, never add a drop changeset)
- Constraint naming: `uq_` / `fk_` prefixes, `_notnull` suffix
- Table registry: register every new table + audit table in `TableNames` + registry entries
- Reference number format: prefix + zero-padded number to 6+ chars (e.g. `OR000042`)
- Entity/JPA rules: no migration info, use `TableNames` constants, no `@Enumerated`
- Auditing columns on all tables; no auditing for lookup tables or immutable fields
- Audit table indexes: on `rev` and `(id, rev)`
- Liquibase file order: `tables/ → version-entries/ → registry-entries/`

**Source**: `instructions.md` DB Design + Changeset + Entity sections,
memory files: `feedback_liquibase_changeset_reuse`, `feedback_constraint_naming`

---

## Skill 4 — `rtss-architecture`

**Trigger**: any task adding a new flow, cross-layer call, Kafka processor, or architectural seam.

**Covers**
- Three-layer structure: platform / organizations / locations
- Cross-layer communication via `api/` packages only (enforced by `ArchitectureTest`)
- Insert flow: `InsertDto → Entity → DomainDto` (released to service layer)
- Update flow: `UpdateDto.applyTo(existing)` with `Optional<TYPE>?` fields; no `@MappingTarget`
- MapStruct `@Context` to avoid O(n) queries in mappers
- DateTimes utility for all `LocalDate` logic — never `LocalDate.now()` or `ZoneOffset.UTC`
- Kafka idempotency: `TransactionEventProcessor.shouldProcess` + unique DB constraint on natural key
- Agent discipline: plan in `plan.txt`, read ≤3 files before first edit, checkpoint every 5 calls

**Source**: `instructions.md` Project Structure + Separation of Concerns + Kafka sections,
memory files: `feedback_datetimes`, `feedback_checkpoint_discipline`, `feedback_autonomous_operation`

---

## Build order

1. `rtss-conventions` — highest reuse, every task touches naming/style
2. `rtss-db` — second most frequent; DB work is common
3. `rtss-sale` — most active domain right now
4. `rtss-purchase` — next most active
5. `rtss-architecture` — needed when adding new flows or cross-layer wiring
6. `rtss-fiscal` — build once `fiscal_period/README.md` has enough rules to warrant it

Each skill file is a Markdown prompt under `.claude/commands/rtss-<name>.md`.
When invoked, it prints the relevant rules as a reminder and identifies which
project files to read next.
