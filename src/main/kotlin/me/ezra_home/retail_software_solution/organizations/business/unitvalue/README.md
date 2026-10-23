# Unit Value Package — Rules & Expectations

This document is the canonical reference for **how a unit of measure behaves** in RTSS. It is intended to be
consumed by humans browsing the codebase **and** by RAG agents that need to answer questions like *"which unit is
stock counted in?"* without re-reading every class.

This package defines the org-scoped catalog of units (`unit_value`) and each unit's place in its group's chain. The
index and graph built from it live in `organizations/business/unitconversion/README.md`.

---

## 1. Scope & Public Surface

```
organizations/business/unitvalue/       — unit catalog, hierarchy, seeding
organizations/business/unitvalue/api/   — public surface (UnitValueFetcher, UnitValueService, UnitChainResolver, DTOs)
```

Cross-domain callers **MUST** go through `api/` (enforced by `ArchitectureTest`). `UnitValueCache`,
`UnitValueMapper`, `UnitValueValidator` and `UnitValueSeeder` are internal to this package.

| Class                | Purpose                                                                                    |
|----------------------|--------------------------------------------------------------------------------------------|
| `UnitValueEntity`    | One row per unit (`unit_value` table)                                                      |
| `UnitValueCache`     | Cached repository access, keyed by org schema                                              |
| `UnitValueValidator` | Insert/update validation                                                                   |
| `SystemUnitValue`    | Seed data enum: every org starts with these units                                          |
| `UnitValueSeeder`    | Persists `SystemUnitValue` entries on org creation, matching existing rows by code         |
| `UnitChainResolver`  | Resolves every unit's root and size in that root; the one place that checks cycles and size |
| `UnitValueFetcher`   | Read-only public surface (names, response DTOs)                                            |
| `UnitValueService`   | Public write surface (create/update/delete)                                                |

## 2. One root per group

Every group has exactly one **root**: the unit with no `baseUnit`. It is fixed when the group is created and is
never searched for at runtime. Following `baseUnit` from any unit ends at the root.

| Group                    | Root                                                                                       |
|--------------------------|--------------------------------------------------------------------------------------------|
| Weight, Volume, Count    | Seeded: Gram, Millilitre, Piece (`SystemUnitValue`)                                        |
| A custom group           | The Piece inserted by `UnitValueService.ensurePieceUnitExists`, with `systemDefined = true` |
| Miscellaneous            | None shared: every MISC unit is its own root                                               |

Because the roots are `systemDefined`, `UnitValueService` rejects editing or deleting them. The auto Piece's code
comes from `SystemUnitValue.pieceCodeForGroup`, which truncates the group name slug, so two long group names can
produce the same code; `validateRootUnitInsert` rejects that collision with a clear error.

## 3. The chain and a unit's size in the root

A unit's `baseUnit` may be **any unit in the same group**, so Tonne → Kilogram → Gram is valid. Each unit is
"1 of this unit = `unitsOfBasePerUnit` of its base unit", a whole number of at least 1. A unit's size in the root is
the product of `unitsOfBasePerUnit` along the chain, computed by `UnitChainResolver` with `Math.multiplyExact`.

`UnitChainResolver` rejects:

- a cycle or self-reference;
- a `baseUnit` that does not exist;
- a chain product that overflows `Long`;
- a size in the root above 1,000,000,000 (`MAX_SIZE_IN_ROOT`). 1,000,000,000 times the largest `numeric(19,4)`
  user quantity is 10^24 − 10^5, which fits `numeric(28,4)`.

`UnitValueValidator` resolves the **whole group** with the edited or new unit applied, so updating a mid-chain unit
re-validates every unit whose chain passes through it.

Units of equal size in one group are allowed, including a unit with `unitsOfBasePerUnit = 1`.

## 4. Insert and update rules

- Outside Miscellaneous, an insert requires a `baseUnit` and `unitsOfBasePerUnit`: the only unit without one is the
  root, and it already exists.
- A Miscellaneous unit cannot have a `baseUnit`, and no unit can name a Miscellaneous unit as its `baseUnit`
  (a base must be in the same group, and MISC units stand alone).
- An update cannot give a root a `baseUnit`. `UnitValueUpdateDto.applyTo` cannot clear a `baseUnit`, because an
  empty `Optional` keeps the existing value.
- A unit's group cannot change (`UnitValueUpdateDto` has no group).
- A used unit's `unitsOfBasePerUnit` and `baseUnit` can change while the chain still ends at the root with whole
  numbers. Stock is counted in the root and lines keep their captured ratio, so neither moves; only the meaning of
  the unit from then on changes.
- A unit that is another unit's `baseUnit` cannot be deleted.
- Deleting a unit group goes through `UnitValueService.deleteAllInUnitGroup`: it rejects the delete while the group
  has any user-defined unit, otherwise removes the group's system-defined units (the auto Piece root) before the
  group itself is deleted.

### Names

Unit names are unique within their group, and group names within the organization, compared on alphanumerics only:
case, whitespace and punctuation are ignored ("Pack Size", "Pack-Size" and "packsize" are the same name).
`StringUtils.isEquivalentOnAlphanumerics` does the comparison (NFKC, Unicode letters and digits), and a name with no
letter or digit is rejected. `StringUtils.isEquivalent` is unchanged and keeps punctuation.

Name checks run against `UnitValueCache` inside an advisory lock: unit mutations lock
`LockNamespaces.UNIT_GROUP` on the group id, and group creation locks `LockNamespaces.UNIT_GROUP_NAME`. Two
concurrent requests cannot both pass the cycle or name check. There is no normalized-name column.

## 5. Miscellaneous

`SystemUnitValue.MISC_PIECE` (code `misc-pc`) is seeded in Miscellaneous and is distinct from Count's Piece (`pc`).
The seeder matches rows by code, so seeding twice leaves one. Miscellaneous is excluded from Piece auto-insert like
every system group (`SystemUnitGroup`).

## 6. Wire surface

`UnitValueInsertDto` / `UnitValueUpdateDto` / `UnitValueResponseDto` all expose `unitsOfBasePerUnit: Long?`
directly.

## 7. Cache invalidation

`UnitValueCache` is keyed by org schema and evicted wholesale (`@CacheEvict(allEntries = true)`) on every
create/save/delete. `UnitValueService` also calls `unitConversionGraphFacade.invalidate()` after every mutation;
that call evicts immediately and again after the transaction completes (see the `unitconversion` README).

Both caches are per node. `UnitValueCache` is Caffeine (`maximumSize=100`, no TTL) and the graph is a per-JVM map,
so eviction reaches only the node that handled the write. In a multi-node deployment the other nodes keep serving
the old unit hierarchy until they restart or evict by size. A line's ratio is captured once, when the line is
created or its unit changes, so a capture on a stale node stores the old factor permanently. This is an accepted
risk, because unit edits are rare admin actions.
