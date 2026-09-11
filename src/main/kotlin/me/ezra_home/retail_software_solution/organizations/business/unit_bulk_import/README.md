# unit_bulk_import

Lets a caller submit a whole payload of unit groups, their unit values, and
cross-group unit conversions in one request (`POST secured/unitgroups/bulk`)
instead of building them up one at a time through `unitgroup`'s, `unitvalue`'s,
and `unitconversion`'s single-item endpoints. This domain only ever imports
from those three domains' `api/` packages — it owns no entities of its own.

## Two phases: validate everything, then save everything

`BulkUnitImportService.bulkImport` runs `BulkUnitImportValidator.validate`
first, which returns a `List<String>` of every problem in the payload —
duplicate/invalid names, duplicate/oversized codes, missing or cross-group
base-unit references, cyclic base-unit chains — instead of throwing on the
first one like the single-item validators do. If that list is non-empty,
`bulkImport` throws `RtsGenericException("Bulk unit import validation
failed", errors)` before anything is saved; `GlobalExceptionHandler` puts the
list in `ApiError.body`. Only once validation is fully clean does phase two
run: groups, then unit values, then conversions, in that order, since each
depends on the previous step's saved ids.

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
- `UnitConversionService.bulkInsertValidatedList` has no cache to evict, but
  still calls `unitConversionGraphFacade.invalidate()` once instead of once
  per conversion.

The `ValidatedList` suffix on all three names is deliberate — it's the
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

## Codes are unique org-wide; names are unique only within a group

`unit_value.code` has a DB-level `unique: true` constraint
(`uq_unit_value_code`) — two unit values anywhere in the org can never share
a code, even across different groups. Unit value **names**, by contrast, are
checked only within the same group (`UnitValueValidator`,
`BulkUnitImportValidator.isDuplicateUnitValueName`) — the same name can
repeat in different groups. Group names remain unique org-wide either way,
and must contain at least one letter or digit (`UnitGroupService.createUnitGroup`'s
`validateNameOnSave`, and `BulkUnitImportValidator.validateGroupNames`) — a
group name that's only symbols/whitespace would otherwise produce an empty
slug for the Piece-code generation below.

## Default "Piece" unit is auto-provisioned, and its code is group-scoped

Any group that isn't Miscellaneous/Weight/Volume (`SystemUnitGroup`) is
expected to have a "Piece" base unit for other values in that group to chain
off. Two flows provision it:

- **Bulk**: only if some unit value in that group actually references the
  reserved marker `BulkUnitImportValidator.PIECE_BASE_UNIT_CODE` ("__piece__")
  as its `baseUnitCode` — `generatePendingUnitValuesForGroup` checks
  `referencesPiece` before adding it.
- **Single-item group creation**: unconditionally, via
  `UnitValueService.ensurePieceUnitExists`, called from
  `UnitGroupService.createUnitGroup` right after the group saves — every new
  (non-excluded) group gets a Piece unit whether or not one was explicitly
  requested, since the single-item `UnitGroupInsertDto` has no unit-values
  payload to check a reference against.

Since codes are globally unique, every group's Piece can't literally be
`SystemUnitValue.PIECE.code` ("pc") — two groups would collide. Both flows
instead call `SystemUnitValue.pieceCodeForGroup(groupName)`, which slugifies
the group name and appends `-pc` (e.g. `"count-pc"`). The function assumes
the name has already been validated to contain a letter or digit (see
above) — without that guarantee the slug could be empty and every
symbol-only-named group's Piece would collide on `"-pc"`.

## Why `UnitGroupService` depends on `UnitValueService`

`UnitGroupService.createUnitGroup` calling `UnitValueService.ensurePieceUnitExists`
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

- `POST bulk` (body: `BulkUnitImportRequestDto` — `unitGroups`, optional
  `unitConversions`) → `BulkUnitImportService.bulkImport`, returning the
  newly created groups (`Collection<UnitGroupResponseDto>`) — not the whole
  org's group list, and not a bulk-specific response shape; deliberately
  matches `createUnitGroup`'s single-item return convention.

Thin delegator; all validation/save-ordering logic lives in
`BulkUnitImportService` and `BulkUnitImportValidator`.
