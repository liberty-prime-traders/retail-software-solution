# Unit Conversion Package — Rules & Expectations

This document is the canonical reference for **how unit conversion
works** in RTSS. It is intended to be consumed by humans browsing the
codebase **and** by RAG agents that need to answer questions like *"how
does the system know how many units are in a case?"* without
re-reading every class.

This package owns two things: the **manual, cross-group conversion
edges** (`unit_conversion` table — e.g. "1 box of nails = 2.3 kg",
relationships that `organizations/business/unitvalue`'s pure hierarchy
can't express), and the **runtime graph** that combines those manual
edges with `unitvalue`'s base-unit chain into a single lookup any two
units in an org can be converted through — even across groups, even
through multiple hops (case → box → unit).

Read `organizations/business/unitvalue/README.md` first — it explains
the base-unit hierarchy this package's graph builds on top of, and why
that package doesn't need the numerator/denominator split this one
does.

---

## 1. Scope & Public Surface

```
organizations/business/unitconversion/       — manual edges, graph builder
organizations/business/unitconversion/api/   — public surface (facade, DTOs, service)
```

| Class                        | Purpose                                                                            |
|------------------------------|------------------------------------------------------------------------------------|
| `UnitConversionEntity`       | One manual cross-group edge (`unit_conversion` table)                              |
| `UnitConversionValidator`    | Insert/update validation — forbids same-group pairs and duplicate edges            |
| `UnitConversionGraphBuilder` | Builds the per-org rational-number graph (BFS), cached by org schema               |
| `UnitConversionGraphFacade`  | Public entry point — `getOrLoad()`, `getFactor()`, `getRatio()`, `invalidate()`    |
| `UnitConversionGraph`        | The built graph — `getTarget()`, `getFactor()`, `getRatio()`, `getFullGraph()`     |
| `ConversionTargetDto`        | One graph edge result — carries `numerator`, `denominator`, and a derived `factor` |
| `UnitConversionService`      | Public write surface (create/update/delete manual edges)                           |

## 2. Why this package needs a real numerator/denominator, unlike unit_value

`unit_value.unitsOfBasePerUnit` collapses to a single whole number
because every row is anchored at "1" (see that package's README).
`unit_conversion.factorNumerator`/`factorDenominator` cannot: this
table exists specifically for relationships that are **not**
integer-reducible on either side (a box of nails doesn't weigh a whole
number of kilograms), and `UnitConversionValidator.validateInsert`
explicitly forbids same-group pairs — those are already connected via
`unit_value`'s hierarchy — so every row here is a genuine cross-group
edge with no anchoring assumption available. `factorNumerator`,
`factorDenominator` must both be `> 0` (validator-enforced); nothing
constrains them to reduce to a whole number.

## 3. The graph: how two arbitrary units get converted

`UnitConversionGraphBuilder.buildGraph()`:

1. Loads every `unit_value` row and every `unit_conversion` row for the
   org.
2. Builds a bidirectional edge map. A `unit_value` edge contributes
   `ConversionRatio(unitsOfBasePerUnit, 1L)` from unit → base unit (and
   its `invert()` the other way). A `unit_conversion` edge contributes
   `ConversionRatio(factorNumerator, factorDenominator)` (and its
   invert).
3. Runs a BFS from every unit, compounding `ConversionRatio.times()`
   along each path — this is what lets a chain like case → box → unit
   resolve to a single ratio even though no direct edge exists between
   case and unit.
4. The result is cached per org schema (`UnitConversionGraphFacade`);
   any write to `unit_value` or `unit_conversion` calls
   `unitConversionGraphFacade.invalidate()` to drop the stale cache
   entry.

Every edge and every compounded path is a `ConversionRatio`
(`util/business/ConversionRatio.kt`) — the graph is built entirely from
integers. `BigDecimal` only appears at the very edge of this package,
as `ConversionTargetDto.factor`, a **derived** convenience field
computed via `Decimals.divideScale4(numerator, denominator)` for
callers that just want a display decimal.

**Use `getRatio()` when you're about to store the result** (an entity
column, a value that will itself be compounded further). **Use
`getFactor()` only when the result is going straight to display or a
one-off calculation that doesn't get persisted** — it's the same
lookup, just collapsed to `BigDecimal` at the very last step.

## 4. Consumers outside this package

`locations/business/location_product/api/LocationProductDataFetcher.
getConversionRatios()` is the seam every location-scoped domain
(purchase, sale, sale_session, stock, stock_transfer) goes through to
ask "what's the ratio between this line's chosen unit and this
product's base unit?" — it calls
`unitConversionGraph.getRatio(unitId, baseUnitId)` per request. What
each domain does with the result (whether it snapshots it onto a line,
how its own wire DTOs shape it) is that domain's concern — see the
README under each of those packages.

A ratio resolved and snapshotted at one point in time does **not**
update retroactively if a unit's definition changes afterward
(`unit_value`/`unit_conversion` edits invalidate the graph cache going
forward, but never touch already-persisted snapshots elsewhere).
`ConversionRatio.isEquivalentTo()` exists for callers that need to
detect that kind of drift by exact cross-multiply rather than a
decimal comparison.

## 5. This package's own wire surface

- `UnitConversionDto` / `UnitConversionInsertDto` /
  `UnitConversionUpdateDto` (CRUD on manual edges) expose flat
  `numerator: Long` / `denominator: Long` siblings — editing an edge
  requires the exact ratio, not a rounded approximation of it.
- `GET secured/unit-conversions/graph` returns the full graph as
  `Map<UUID, Map<UUID, ConversionTargetDto>>`, i.e. numerator,
  denominator, and a derived `factor` per reachable unit pair.
