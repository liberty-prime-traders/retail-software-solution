# Role Assignment — Rules & Expectations

This document is the canonical reference for **how role-based
authorization works** in RTSS. The design is a single pattern
duplicated across three tiers, plus one orchestrator that ties them
together:

```
platform/business/role_assignment/       — PLATFORM tier (this package; also the orchestrator)
organizations/business/role_assignment/  — ORGANIZATION tier
locations/business/role_assignment/      — LOCATION tier
```

Each tier package has the identical shape: `<Tier>RoleAssignmentEntity`
(`@Audited`, extends `util/model/RoleAssignmentEntity`), a
`<Tier>RoleAssignmentRepository`, a `<Tier>RoleCache` (per-schema
Caffeine cache, keyed by userId, fully invalidated on any write — these
tables are small and read on every request, so this isn't a
correctness compromise), and a `<Tier>RoleAssignmentService`
(`getRoles`/`holds`/`assign`/`remove`). Read `organization_user`'s
README (there isn't one — it's the membership package `assign()`
resolves against) and `platform/business/auth/README.md` (login and
the per-request authority refresh that consumes this package) as
companion reading.

## 1. `RtsRole` / `RtsRoleNames` — two files, kept in sync by convention only

`util/enums/RtsRole.kt` is the actual enum: `code` (a short DB
persistence code, unrelated to Kotlin's `.name`), `tier`
(`SchemaLevel.PLATFORM`/`ORGANIZATION`/`LOCATION` — which
`<Tier>RoleAssignmentService` a role's rows live in), and
`requiredRoles` (§2). `util/enums/RtsRoleNames.kt` is a plain
`const val` string per entry, existing **only** because a Kotlin
annotation argument (`@PreAuthorize("hasRole('...')")`) must be a
compile-time constant — `RtsRole.SOME_ROLE.name` is a property/method
call and will not compile inside an annotation, so there is no way to
have `@PreAuthorize` reference the enum type directly. This mirrors
`util/model/TableNames.kt` next to `util/model/TableName.kt` — same
kind of split, same reason.

**Nothing enforces these two files stay in sync except
`RtsRoleNamesTest`** (`src/test/.../util/enums/RtsRoleNamesTest.kt`),
which asserts every `RtsRoleNames` constant's *value* matches exactly
one `RtsRole.entries` name and vice versa. Add a role in one file
without the other and this test — not the compiler — is what catches
it. `TableNames`/`TableName` has no equivalent test; don't assume the
same is true there.

## 2. `requiredRoles` — a write-time dependency check, not a hierarchy

A role can declare other roles that must already be held before it can
be granted (`RoleResolutionService.assignRole`: throws if any
`role.requiredRoles` entry isn't held). Nothing in this codebase
currently populates `requiredRoles` with anything — the mechanism
exists and is enforced, but every `RtsRole` entry today has an empty
list. When you do use it: removing a role cascades to remove every
*other* role currently held that lists it as required
(`RoleResolutionService.removeRoleAndDependents`, recursive) — this is
the only cascade that crosses `RtsRole` entries; it says nothing about
membership (§3 is a separate, unrelated cascade).

## 3. Org/location roles require an active membership row — structurally

`org_role_assignment` and `location_role_assignment` each carry a real
foreign key (`org_user_id` / `location_user_id`) to the membership row
in the same schema (`organization_user.id` / `location_user.id`), in
addition to the plain `user_id` every tier's table has. `user_id` is
kept — not replaced — because `RoleResolutionService`'s per-request
`getEffectiveRoles`/`holds` lookups (called from `TenantFilter` on
every request) go by bare user id; requiring a join there on every
request for a check this hot wasn't worth it. The FK column exists
purely to make "no org/location role without an active membership"
structural rather than something re-checked ad hoc in service code.

Two things enforce this, redundantly on purpose:

1. **`RoleResolutionService.assignRole`** resolves the caller's active
   membership id via `OrganizationUserService.getActiveMembershipId` /
   `LocationUserService.getActiveMembershipId` *before* calling
   `Org/LocationRoleAssignmentService.assign(...)`. Both throw
   `RtsGenericException` if the user has no active (`endOn == null`)
   membership row — this is the actual enforcement point; a failed
   role grant here never reaches the database.
2. **The FK constraint itself** is the backstop if that check is ever
   bypassed by a future direct-insert code path.

The reverse direction — membership ending — hard-deletes rather than
soft-deletes: `OrganizationUserService.terminateOrganizationUsers` and
`LocationUserService.terminateLocationUsers` both call
`Org/LocationRoleAssignmentService.removeAllRoles(userId)` in the same
method (so the same transaction) right after setting `endOn`. There is
deliberately no `endOn`/status column on the role assignment tables
themselves — a row existing *at all* means the grant is currently
active; `@Audited` (Envers, `_aud` tables) is the history/audit view
for what used to be granted, not a status flag on the live table.

**Why this doesn't create a circular dependency**: it would be natural
to expect `Org/LocationRoleAssignmentService` to depend on
`Organization/LocationUserService` (to resolve the membership id
inside `assign()`) while `Organization/LocationUserService` also
depends on `Org/LocationRoleAssignmentService` (to cascade-delete on
terminate) — a real constructor-injection cycle. It doesn't, because
the membership-id resolution was deliberately placed one level up, in
`RoleResolutionService` (which already depended on both org/location
role services and gained the two membership services alongside them),
not inside `Org/LocationRoleAssignmentService` itself. That service's
`assign()` takes the already-resolved membership id as a plain
parameter and does no lookup of its own. Keep it that way — moving the
lookup down into the tier service to "simplify the call site" would
reintroduce the cycle.

## 4. Where the effective role set actually gets consulted

`RoleResolutionService.getEffectiveRoles` always includes platform
roles, and includes org/location roles only if
`SessionContextProvider.getOrganizationIdOrNull()` /
`getLocationIdOrNull()` is non-null for the current request — this
service never fans out across every org/location schema looking for a
user's roles; it only ever looks at whatever schema the current
request has already resolved. `TenantFilter` is what calls this once
per request and rewrites the security context's authorities from the
result — see `platform/business/auth/README.md` §4 for the full
per-request lifecycle and why the session JWT itself never carries
roles.
