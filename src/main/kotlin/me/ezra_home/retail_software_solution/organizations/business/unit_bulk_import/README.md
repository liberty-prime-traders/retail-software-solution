# unit_bulk_import

Lets a caller submit a whole payload of unit groups and their unit values in one
request (`POST secured/unitgroups/bulk`) instead of building them up one at a
time through `unitgroup`'s and `unitvalue`'s single-item endpoints. This domain
only ever imports from those domains' `api/` packages — it owns no entities of
its own. There are no manual conversions to import (see `unitconversion`).

## Two phases: validate everything, then save everything

`BulkUnitImportValidator` orchestrates and owns the unit-value checks; two stateless helpers hold the rest:
`BulkUnitGroupValidator` (group names, `baseUnitName`, `baseUnitCode`, generated base unit codes) and `BulkUnitChainValidator`
(cycles, chain sizes).

`BulkUnitImportService.bulkImport` runs `BulkUnitImportValidator.validate`
first, which returns a `List<String>` of every problem in the payload —
duplicate/invalid names (compared on alphanumerics), duplicate/oversized/reserved (`"__piece__"`) unit value codes,
missing or cross-group base-unit references, a unit outside Miscellaneous
without a base unit, a Miscellaneous unit with one, a payload unit named
named like the group's effective base unit name in a custom group, a
`baseUnitName` or `baseUnitCode` on a system group (or a payload group reusing one), a
`baseUnitName` without a letter or digit, a `baseUnitCode` that is reserved, too long or already taken, two groups
whose root unit codes collide,
cyclic base-unit chains, and chains whose size in the root exceeds 1,000,000,000
(`UnitChainResolver`) — instead of throwing on the
first one like the single-item validators do. If that list is non-empty,
`bulkImport` throws `RtsGenericException("Bulk unit import validation
failed", errors)` before anything is saved; `GlobalExceptionHandler` puts the
list in `ApiError.body`. Only once validation is fully clean does phase two
run: groups, then unit values, in that order, since units depend on the
saved group ids.

## Why bulk save paths exist instead of looping single-item creates

`UnitGroupService.createUnitGroup` and `UnitValueService.createUnitValue`
each evict their cache (`UnitGroupCache`/`UnitValueCache`,
`@CacheEvict(allEntries = true)`) on every call. Looping either for N rows
would mean N full-cache evictions for one import. Instead:

- `UnitGroupService.bulkCreateValidatedList` and
  `UnitValueService.bulkCreateValidatedList` map straight to entities (no
  per-item validation — the whole batch was already validated in phase one)
  and persist through `UnitGroupCache.saveAll` / a new `UnitValueCache.saveAll`,
  each a single `@CacheEvict`.

The `ValidatedList` suffix on both names is deliberate — it's the
caller's contract that the list has already been validated, so the method
body can skip straight to persistence. See `.claude/instructions.md`'s
Comments section for why this is a naming convention, not a comment above
the method.

## Unit values: pre-generated ids, topological order, one `saveAll`

`unit_value.base_unit` is a self-referential FK. A single `saveAll` issues
inserts in list order, so a child row would violate the FK if it's inserted
before its parent. `BulkUnitImportService.saveValidatedUnitValues` avoids
looping (and re-evicting per row) by:

1. Building one `PendingUnitValue` per unit value across every group
   (`generatePendingUnitValuesForGroup`), keyed by `code` alone — codes are
   unique org-wide (see below), so no group-qualifying key is needed.
2. Topologically sorting that list (`topologicallyOrder`, reusing
   `UnitValueDependencyGraph` — the same class the validator uses for cycle
   detection in phase one, which is why this step never actually finds a
   cycle: phase one already proved the graph acyclic).
3. Walking the sorted list once, assigning each a fresh `UUID.randomUUID()`
   and resolving its `baseUnitCode` to an already-assigned id via a running
   `code -> id` map (seeded from `UnitValueFetcher.getAllUnitValues()` for
   codes that already existed before this import).
4. Passing the fully-resolved `UnitValueBulkSaveEntry` list to
   `UnitValueService.bulkCreateValidatedList` in one call — parents are
   earlier in the list than children, so the single `saveAll` never violates
   the FK.

## A payload matching system-defined data is a reuse, not a conflict

Every org is bootstrapped with the `SystemUnitGroup` and `SystemUnitValue`
rows (`UnitGroupSeeder`, `UnitValueSeeder`) - e.g. "Weight" and "kg" already
exist in every org before any bulk import runs. Without special handling, a
payload that names a standard unit would always fail with an "already
exists" error, since the validator otherwise treats any name/code collision
against the database as invalid.

Instead, `BulkUnitGroupValidator.validateGroupNames` and `BulkUnitImportValidator.validateUnitValues`
check whether the colliding database row is `systemDefined`. If it is, the
payload entry is treated as a reuse: no error is added, and validation of
that entry stops there (its own name/base-unit/etc. fields are never
checked, since it won't be created). `BulkUnitImportService` mirrors this at
save time - `findExistingSystemDefinedGroup` and `existingSystemDefinedCodes`
resolve a reused entry to its existing id instead of passing it to
`bulkCreateValidatedList`, so the same code never gets inserted twice. A reused
code must be listed under the group that owns it: a system code under any other
group is rejected, because the entry is skipped at save and would otherwise let that
group's units pass the base-unit group check against a unit that never joins it. A
payload that lists the same system-defined code twice is still rejected as a
payload-internal duplicate; only the database-collision error is suppressed.

## Unit group names are unique org-wide, on every write path

A unit group name is unique org-wide on its letters and digits alone (`UnitGroupNameRules`), so "Pack Size" and
"pack-size" collide. Every path that writes groups enforces it under the `UNIT_GROUP_NAME` advisory lock:
`UnitGroupService.createUnitGroup` and `updateUnitGroup`, `UnitGroupService.bulkCreateValidatedList` (which guards itself,
so a caller that skips `BulkUnitImportValidator` still cannot duplicate a name), and `UnitGroupSeeder`. The seeder keys on
`code`, so a custom group already holding a system group's name stops the seed with an error rather than adding a second
group; resolve it by renaming or removing the custom group. `unit_group.name_key` holds `StringUtils.alphanumericKey(name)`, set by the entity on persist and update, and
`uq_unit_group_name_key` makes the database reject exactly what the application check rejects.

## Codes are unique org-wide; names are unique only within a group

`unit_value.code` has a DB-level `unique: true` constraint
(`uq_unit_value_code`) — two unit values anywhere in the org can never share
a code, even across different groups. Unit value **names**, by contrast, are
checked only within the same group (`UnitValueValidator`,
`BulkUnitImportValidator.isDuplicateUnitValueName`) — the same name can
repeat in different groups. Both comparisons use
`StringUtils.isEquivalentOnAlphanumerics`, so "Pack Size" and "Pack-Size"
collide. A payload group name that matches a system group under
`StringUtils.isEquivalent` is a reuse, but one that only matches on
alphanumerics ("Co-unt") is rejected as a duplicate. Group names remain unique
org-wide either way, and must contain at least one letter or digit (`UnitGroupService.createUnitGroup`'s
`validateNameOnSave`, and `BulkUnitGroupValidator.validateGroupNames`) — a
group name that's only symbols/whitespace would otherwise produce an empty
slug for the root unit code generation below.

## The base unit is auto-provisioned, named by the caller, and its code is group-scoped

Every group outside the system groups (`SystemUnitGroup`) has exactly one
root: the base unit provisioned when the group is created. Every other unit in
the group chains off it, directly or through another unit, so a payload unit in
a custom group references it with the reserved marker
`RootUnitDefinition.PIECE_BASE_UNIT_CODE` ("__piece__") or, when the group supplies one, with the group's explicit
`baseUnitCode`; both resolve to the same root. The root's name
is the group's optional `baseUnitName` (`UnitGroupBulkInsertDto` /
`UnitGroupInsertDto`; blank is absent) and defaults to "Piece". A payload unit
in a custom group whose name is equivalent on alphanumerics to that effective
base unit name is rejected, and the error names it. `baseUnitName` is an error
on a system group or a payload group that reuses a system-defined group (their
roots are seeded), and must contain a letter or digit.

Both flows build the root through `RootUnitDefinition.forGroup` (name, code via `effectiveBaseUnitCode`,
description, `systemDefined = true`) so they cannot drift:

- **Bulk**: for every new custom group, whether or not a unit references the
  marker (`generatePendingUnitValuesForGroup`).
- **Single-item group creation**: via `UnitValueService.ensureBaseUnitExists`,
  called from `UnitGroupService.createUnitGroup` right after the group saves.

A Miscellaneous payload unit cannot carry a `baseUnitCode`, and a unit in any
other group must.

### The root unit's code

A group may supply the code with the optional `baseUnitCode` (`UnitGroupBulkInsertDto` / `UnitGroupInsertDto`; blank
is absent), so callers that load products can reference the root without reproducing the generation rule. Without
one, the code is generated.

Since codes are globally unique, every group's root can't literally be `SystemUnitValue.PIECE.code` ("pc") — two
groups would collide. The generated code comes from `SystemUnitValue.baseUnitCodeForGroup(groupName)`, which
slugifies the group name and appends `-base` (e.g. `"eggs-base"`), whatever the base unit is named. The function
assumes the name has already been validated to contain a letter or digit (see above) — without that guarantee the
slug could be empty and every symbol-only-named group's root would collide on `"-base"`. Only
`RootUnitDefinition.effectiveBaseUnitCode` chooses between the two, so no other class computes a root code.

A supplied `baseUnitCode` follows the unit value code rules (at most `SystemUnitValue.MAX_CODE_LENGTH` characters),
cannot be `"__piece__"`, and must differ from every payload unit code, every other payload group's root code
(supplied or generated) and every existing unit code. `BulkUnitGroupValidator.validateBaseUnitCodes` runs the
uniqueness check on the effective root code of every custom group, so the generated-code collision error only fires
for groups that did not supply one. In the single-item flow `UnitGroupService.createUnitGroup` applies the
same length, reserved-marker and uniqueness rules before saving the group
(`UnitValueService.validateRootUnitCodeBeforeGroupCreate`), and `UnitValueValidator.validateRootUnitInsert`
re-checks them under the group lock.

Chain, cycle and size validation and the topological ordering treat the supplied code as the group's root node:
`BulkUnitChainValidator` and `BulkUnitImportService` resolve the root code through
`RootUnitDefinition.effectiveBaseUnitCode`, and `"__piece__"` maps to it.

## Why `UnitGroupService` depends on `UnitValueService`

`UnitGroupService.createUnitGroup` calling `UnitValueService.ensureBaseUnitExists`
made `unitgroup` depend on `unitvalue`, while `UnitValueValidator` already
depended on `unitgroup` (to check a unit value's `unitGroupId` exists) — a
Spring circular-bean dependency. Rather than papering over it with `@Lazy`,
`unitgroup/api/UnitGroupDataFetcher` was added as a narrow, existence-only
read wrapper around `UnitGroupCache`; `UnitValueValidator` now depends on
that instead of the full `UnitGroupService`, so the cycle is gone rather
than just deferred. This lives in `unitgroup`, not here, but the need for it
came directly from this feature.

## REST surface

`UnitGroupEndpoint` (`secured/unitgroups`):

- `POST bulk` (body: `BulkUnitImportRequestDto` — `unitGroups`) → `BulkUnitImportService.bulkImport`, returning the
  newly created groups (`Collection<UnitGroupResponseDto>`) — not the whole
  org's group list, and not a bulk-specific response shape; deliberately
  matches `createUnitGroup`'s single-item return convention.

Thin delegator; all validation/save-ordering logic lives in
`BulkUnitImportService` and `BulkUnitImportValidator`.
