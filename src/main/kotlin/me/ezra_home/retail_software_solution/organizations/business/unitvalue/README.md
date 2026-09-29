# Unit Value Package — Rules & Expectations

This document is the canonical reference for **how a unit of measure
behaves** in RTSS. It is intended to be consumed by humans browsing the
codebase **and** by RAG agents that need to answer questions like *"why
does HALF_LITRE convert to MILLILITRE and not LITRE?"* without re-reading
every class.

This package defines the org-scoped catalog of units (`unit_value`) and
each unit's relationship to its group's atomic base unit. It is the
smaller, simpler half of the unit-conversion story — see
`organizations/business/unitconversion/README.md` for the graph that
combines this package's hierarchy with manually-declared cross-group
edges, and `util/business/ConversionRatio.kt` for the shared
numerator/denominator value type both packages use.

---

## 1. Scope & Public Surface

```
organizations/business/unitvalue/       — unit catalog, hierarchy, seeding
organizations/business/unitvalue/api/   — public surface (UnitValueFetcher, UnitValueService, DTOs)
```

Cross-domain callers **MUST** go through `api/` (enforced by
`ArchitectureTest`). `UnitValueCache`, `UnitValueMapper`,
`UnitValueValidator`, `UnitValueSeeder`, and `SystemUnitValue` are
internal to this package.

| Class                | Purpose                                            |
|----------------------|----------------------------------------------------|
| `UnitValueEntity`    | One row per unit (`unit_value` table)              |
| `UnitValueCache`     | Cached repository access, keyed by org schema      |
| `UnitValueValidator` | Insert/update validation                           |
| `SystemUnitValue`    | Seed data enum — every org starts with these units |
| `UnitValueSeeder`    | Persists `SystemUnitValue` entries on org creation |
| `UnitValueFetcher`   | Read-only public surface (names, response DTOs)    |
| `UnitValueService`   | Public write surface (create/update/delete)        |

## 2. The core invariant: always "1 of this unit = N of its base unit"

Every `unit_value` row with a non-null `baseUnit` is, by construction,
framed as **"1 of this unit equals `unitsOfBasePerUnit` of its base
unit."** The "1" side is never stored — it's implicit. That means the
table only needs **one** integer column
(`unitsOfBasePerUnit: Long?`), not a numerator/denominator pair, to
represent the relationship exactly.

This only works because the invariant is enforced:

- `UnitValueValidator` rejects `unitsOfBasePerUnit < 1` on both insert
  and update. A unit can never be defined as "0.5 of its base unit" —
  if you need a fractional relationship, point the smaller unit at the
  group's true atomic base instead (see below).
- Units with no `baseUnit` (e.g. `GRAM`, `MILLILITRE`, `PIECE`) are a
  group's **atomic root** — the unit everything else in that group is
  ultimately expressed in whole numbers of.

**Concrete example — why `HALF_LITRE` points at `MILLILITRE`, not
`LITRE`:** a half-litre is 0.5 litres, which would violate the
whole-number invariant if `LITRE` were its base unit. Instead
`HALF_LITRE.baseUnit = MILLILITRE` with `unitsOfBasePerUnit = 500` — a
clean whole number. **Rule of thumb: always anchor a unit's `baseUnit`
to its group's ultimate atomic root, never to an intermediate unit,**
even when the intermediate unit "looks" more natural — otherwise you
risk landing on a fraction the validator will reject.

See `SystemUnitValue.kt` for the full seed hierarchy
(WEIGHT: `GRAM` ← `KILOGRAM` ← `TONNE`; VOLUME: `MILLILITRE` ←
`LITRE`, `HALF_LITRE`; COUNTABLE: `PIECE` ← `DOZEN`, `CRATE`, `TRAY`).

## 3. Why this doesn't generalize to unit_conversion

`organizations/business/unitconversion` (the manual, cross-group edge
table) is a genuinely free ratio — "1 box of nails = 2.3 kg" is not
integer-reducible on either side, and `UnitConversionValidator`
explicitly forbids same-group pairs (same-group units are already
connected via this package's hierarchy). That table stores a full
`factorNumerator`/`factorDenominator` pair. Don't be tempted to apply
this package's single-column simplification there.

## 4. Wire surface

`UnitValueInsertDto` / `UnitValueUpdateDto` / `UnitValueResponseDto`
all expose `unitsOfBasePerUnit: Long?` directly — it's already a plain
whole number, so there's no derived decimal to compute for display.

## 5. Cache invalidation

`UnitValueCache` is keyed by org schema and evicted wholesale
(`@CacheEvict(allEntries = true)`) on every create/save/delete.
`UnitValueService` also calls
`unitConversionGraphFacade.invalidate()` after every mutation — the
graph in `unitconversion/` is built from this package's data, so a
stale unit hierarchy would otherwise linger in the graph cache.
