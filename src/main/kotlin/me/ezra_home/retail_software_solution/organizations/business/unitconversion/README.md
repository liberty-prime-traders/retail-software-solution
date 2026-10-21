# Unit Conversion Package — Rules & Expectations

This package owns the **per-organization unit index and conversion graph**. It is derived entirely from
`unit_value` rows (`organizations/business/unitvalue`); nothing about conversion is stored here.

Read `organizations/business/unitvalue/README.md` first: it defines the one-root-per-group model this package
indexes.

---

## 1. Scope & Public Surface

```
organizations/business/unitconversion/       — graph builder
organizations/business/unitconversion/api/   — public surface (facade, graph, DTO)
```

| Class                        | Purpose                                                                                       |
|------------------------------|-----------------------------------------------------------------------------------------------|
| `UnitConversionGraphBuilder` | Builds the per-org index and graph from the unit rows, cached by org schema                   |
| `UnitConversionGraphFacade`  | Public entry point: `getOrLoad()`, `getFactor()`, `getRatio()`, `invalidate()`                 |
| `UnitConversionGraph`        | The built index: `getRatio()`, `getFactor()`, `getStorageUnitId()`, `getSizeInRoot()`, `getFullGraph()` |
| `ConversionTargetDto`        | One graph entry: target unit, reduced `numerator` / `denominator`, derived `factor`            |

There is no manual conversion. The `unit_conversion` table, its entity, service and CRUD routes do not exist.
`GET secured/unit-conversions/graph` is the only route (`UnitConversionEndpoint`).

## 2. What a conversion is

Units convert **only within their group**. A unit's size in its group's root is the product of
`unitsOfBasePerUnit` along its `baseUnit` chain, resolved by `UnitChainResolver` (`unitvalue/api`) with
`Math.multiplyExact`. The ratio from unit A to unit B in one group is `size(A) / size(B)`, reduced.

A Miscellaneous unit stands alone (`UnitValueValidator` forbids a base unit), so it converts only to itself, never to
the other units of the Miscellaneous group.

Requesting a ratio between units of different groups, or between two Miscellaneous units, throws `RtsGenericException`
(`UnitConversionGraph.getTarget` finds no entry).

## 3. The index and the graph

`UnitConversionGraphBuilder.buildGraph()`:

1. Loads every unit through `UnitValueFetcher`.
2. Resolves every unit's placement (`rootUnitId`, `sizeInRoot`) in one pass. A cycle, a missing base unit, an
   overflow or a size above 1,000,000,000 throws; `UnitValueValidator` rejects these on write, so a read never
   sees them.
3. For every group, builds an entry for every ordered pair of its units, including a unit to itself. A Miscellaneous
   unit (`UnitGroupDataFetcher.isMiscellaneous`) gets only its self entry.

The result is cached per organization schema. Lookups read the index; nothing walks a chain at call time:

- `getStorageUnitId(unitId)`: the root of the unit's group. This is the product's **storage unit**.
- `getSizeInRoot(unitId)`: the unit's size in that root.
- `getRatio(from, to)` / `getFactor(from, to)`: from the pair entries.

The graph route returns `Map<fromUnitId, Map<toUnitId, ConversionTargetDto>>`. The UI multiplies with it when a
user picks a unit.

## 4. Invalidation

`UnitConversionGraphBuilder.invalidate()` evicts immediately, so reads in the writing transaction see their own
changes, and registers an `afterCompletion` eviction. A request that rebuilds the index between the immediate
eviction and the commit would otherwise cache the old committed rows, and a rebuild inside a transaction that
rolls back would cache rows that never existed. The cache is per JVM.

Every write in `UnitValueService` invalidates.
