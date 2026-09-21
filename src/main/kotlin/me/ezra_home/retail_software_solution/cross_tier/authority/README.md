# Authority — Rules & Expectations

This document is the canonical reference for **how role-based
authorization works** in RTSS. The design is a single pattern
duplicated across three tiers, plus four single-responsibility
orchestrator services (all in this package, `cross_tier/authority/`)
that tie them together — `EffectiveAuthorizationService` (read),
`RoleGrantService` and `PermissionGrantService` (command, one per
grant type), and `AuthorizationRequestService` (REST-facing tier
validation, composing the two grant services — §8). These four used to
be one class, `PermissionResolutionService`; it was split once it
accumulated all four responsibilities at once.

This package lives under `cross_tier/`, not under any one tier's
`business/` package, because every class here is used identically by
all three tiers (or, for the DTOs, is returned by services from all
three) — `ArchitectureTest`'s domain-isolation rule only applies inside
a `<tier>/business/<domain>` package, and code with no single tier
owner doesn't belong there. What each tier *does* own is its own
persistence-backed grant service:

```
platform/business/role_assignment/       — PLATFORM tier persistence (entity/repository/cache/service)
organizations/business/role_assignment/  — ORGANIZATION tier persistence
locations/business/role_assignment/      — LOCATION tier persistence
cross_tier/authority/                    — this package: tier-agnostic orchestration + shared DTOs
```

Each tier package has the identical shape: `<Tier>RoleAssignmentEntity`
(`@Audited`, extends `util/model/RoleAssignmentEntity`), a
`<Tier>RoleAssignmentRepository`, a `<Tier>RoleCache` (per-schema
Caffeine cache, keyed by userId, fully invalidated on any write — these
tables are small and read on every request, so this isn't a
correctness compromise), and a `<Tier>RoleAssignmentService`
(`getRoles`/`holds`/`assign`/`remove`), exposed via that tier's own
`.api` package. This package's orchestrator services depend on all
three tiers' `<Tier>RoleAssignmentService`/`<Tier>PermissionAssignmentService`
to do their job — see `platform/business/role_assignment/README.md`
for what stayed behind at platform tier and why. Read
`organization_user`'s README (there isn't one — it's the membership
package `assign()` resolves against) and
`platform/business/identity/README.md` (login and the per-request
authority refresh that consumes this package) as companion reading.

## 1. Two enums: `RtsRole` (grantable bundle) and `RtsPermission` (atomic, checked)

`RtsRole` is what actually gets assigned to a user (via the
`<tier>_role_assignment` tables) — a named bundle with a `permissions:
Set<RtsPermission>` field. `RtsPermission` (`util/enums/RtsPermission.kt`)
is the atomic thing `@PreAuthorize` actually checks — `ADD_USER_TO_LOCATION`,
`VIEW_USERS_OF_ORG`, etc. **A user is never granted an `RtsPermission`
directly through a role** — permissions arrive either bundled inside a
role (§5) or as an independent add-on (§6). `PLATFORM_ADMIN` bundles
exactly one permission, `CREATE_ORGANIZATION` — moved from being its
own `RtsRole` (it used to be, unbundled) to a platform-tier
`RtsPermission` once it became clear a platform user might reasonably
need *just* the ability to create an organization, granted as an
add-on (§6), without full `PLATFORM_ADMIN`. Not every role needs a
non-empty `permissions` set — `PLATFORM_ADMIN` is still also checked
directly with `hasRole(...)` everywhere else in this codebase, never
`hasAuthority(...)`, for its broader admin actions that have no
corresponding permission of their own.

Each enum has a `code` (short DB persistence code, unrelated to
Kotlin's `.name`) and `tier` (`SchemaLevel.PLATFORM`/`ORGANIZATION`/
`LOCATION` — which `<Tier>RoleAssignmentService`/permission-assignment
table a grant's rows live in). `RtsRole` additionally has
`requiredRoles` (§2).

**`RtsRoleNames`/`RtsPermissionNames`** are plain `const val` string
mirrors of the two enums, existing **only** because a Kotlin
annotation argument (`@PreAuthorize("hasRole('...')")`,
`hasAuthority('...')`) must be a compile-time constant —
`RtsRole.SOME_ROLE.name` is a property/method call and will not
compile inside an annotation, so there is no way to have
`@PreAuthorize` reference the enum type directly. This mirrors
`util/model/TableNames.kt` next to `util/model/TableName.kt` — same
kind of split, same reason.

**Nothing enforces the enum/const-object pairs stay in sync except
`RtsRoleNamesTest`/`RtsPermissionNamesTest`**
(`src/test/.../util/enums/`), which each assert the const object's
values match the corresponding enum's entry names exactly, both
directions. Add an entry in one file without the other and one of
these tests — not the compiler — is what catches it.
`RtsRoleBundleCoverageTest` (same directory) is a third, different
guard: every `RtsPermission` must appear in at least one `RtsRole`'s
`permissions` set, so a permission that can only ever be granted as an
add-on (§6) — never through any role — fails loudly instead of
quietly becoming impossible to bundle. `TableNames`/`TableName` has no
equivalent test; don't assume the same is true there.

## 2. `requiredRoles` — a write-time dependency check, not a hierarchy

A role can declare other roles that must already be held before it can
be granted (`RoleGrantService.assignRole`: throws if any
`role.requiredRoles` entry isn't held). Nothing in this codebase
currently populates `requiredRoles` with anything — the mechanism
exists and is enforced, but every `RtsRole` entry today has an empty
list. When you do use it: removing a role cascades to remove every
*other* role currently held that lists it as required
(`RoleGrantService.removeRoleAndDependents`, recursive) — this is
the only cascade that crosses `RtsRole` entries; it says nothing about
membership (§3 is a separate, unrelated cascade).

## 3. Org/location roles require an active membership row — structurally

`org_role_assignment` and `location_role_assignment` each carry a real
foreign key (`org_user_id` / `location_user_id`) to the membership row
in the same schema (`organization_user.id` / `location_user.id`), in
addition to the plain `user_id` every tier's table has. `user_id` is
kept — not replaced — because `EffectiveAuthorizationService`'s per-request
`getEffectiveRoles` lookups (called from `TenantFilter` on every
request) go by bare user id; requiring a join there on every request
for a check this hot wasn't worth it. The FK column exists purely to
make "no org/location role without an active membership" structural
rather than something re-checked ad hoc in service code.

Two things enforce this, redundantly on purpose:

1. **`RoleGrantService.assignRole`** resolves the caller's active
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
`RoleGrantService` (which depends on both org/location role services
and the two membership services), not inside
`Org/LocationRoleAssignmentService` itself. That service's
`assign()` takes the already-resolved membership id as a plain
parameter and does no lookup of its own. Keep it that way — moving the
lookup down into the tier service to "simplify the call site" would
reintroduce the cycle.

## 4. Where the effective role/permission set actually gets consulted

`EffectiveAuthorizationService.getEffectiveRoles`/`getEffectivePermissions`
always include platform-tier grants, and include org/location grants
only if `SessionContextProvider.getOrganizationIdOrNull()` /
`getLocationIdOrNull()` is non-null for the current request — neither
method ever fans out across every org/location schema looking for a
user's grants; each only ever looks at whatever schema the current
request has already resolved. `TenantFilter` calls both once per
request and rewrites the security context's authorities from the
result — `ROLE_${role.name}` for each held role, plus the bare
`permission.name` (no prefix) for each entry in
`getEffectivePermissions()` — see `platform/business/identity/README.md`
§4 for the full per-request lifecycle and why the session JWT itself
never carries either.

## 5. Roles batch-grant/revoke their bundled permissions — for free

Because a role's permission set is code-defined on the `RtsRole` entry
itself, there is no per-permission bookkeeping when a role is
granted or revoked. Assigning `MANAGE_ORGANIZATION_USERS` is **one row**
insert into `org_role_assignment`; every permission in its bundle
becomes effective immediately because `getEffectivePermissions`
expands the bundle live, every call, from whatever roles are
currently held — nothing is materialized per-permission at grant
time. Revoking is the same in reverse: **one row delete**, and the
whole bundle disappears, with no stale-permission cleanup step
required anywhere. This is *why* `org_permission_assignment` (§6) had
to be a separate table rather than just more rows in
`org_role_assignment` typed loosely — a bundle's permissions need to
vanish as a unit when the role goes, while an add-on grant must not.

`MANAGE_ORGANIZATION_USERS`/`VIEW_ORGANIZATION_USERS` (`ORGANIZATION` tier) and
`PLATFORM_ADMIN` (`PLATFORM` tier, bundling only `CREATE_ORGANIZATION`
— see §1) are the only `RtsRole` entries with a non-empty `permissions`
set today. There is currently no `RtsRole` at `LOCATION` tier at all, so
`location_role_assignment` is fully scaffolded (mirrors the org/platform
shape, including the `location_user_id` membership FK from §3) but
has no live entry to grant through it yet.

## 6. Add-on permissions — granted independently of any role, on purpose

`org_permission_assignment` is a second table, structurally identical
to `org_role_assignment` (same membership-FK pattern from §3, same
`@Audited`, same cascade-delete-on-membership-termination), except it
stores an `RtsPermission` directly rather than an `RtsRole`. This is
the mechanism for granting one specific permission to a user *without*
promoting them to whichever role happens to bundle it —
`PermissionGrantService.assignPermission`/`removePermission`.

The critical property this buys: **revoking a role never touches an
add-on grant, even when the role's bundle includes that exact
permission.** `getEffectivePermissions` is a plain union of
"permissions expanded from held roles" and "permissions held directly"
(`OrgPermissionAssignmentService.getPermissions`) — the two are never
merged into one row, so there's no way for a role revocation to
accidentally sweep up a permission that was actually granted for an
independent reason. If you ever find yourself tempted to "simplify"
this by writing an add-on grant into `org_role_assignment` as a
single-permission pseudo-role, don't — that reintroduces exactly the
ambiguity this split exists to avoid.

All three tiers now have their own permission-assignment stack
(`Platform`/`Org`/`LocationPermissionAssignmentService`) — each built
only once a real tier-appropriate permission actually needed it:
`CREATE_ORGANIZATION` for platform, `MANAGE_ORGANIZATION_ACCESS` for org (§7),
`MANAGE_LOCATION_ACCESS` for location (§7). The platform one is the
odd one out structurally — no membership FK (§3), no `@Audited`,
because platform tier has no membership concept at all; it mirrors
`PlatformRoleAssignmentEntity`'s existing shape instead of the
org/location one. Keep following this "don't build ahead of need"
rule for anything still missing — nothing else currently needs it.

## 7. `MANAGE_ORGANIZATION_ACCESS` / `MANAGE_LOCATION_ACCESS` — granting the ability to grant

These two permissions gate who can call the assignment endpoints for their
respective tier (§8) — i.e. who can grant/revoke *other* roles and
permissions within that org or location. They are deliberately **add-on
only**: `RtsRoleBundleCoverageTest` (§1) exempts them by name, with a
comment explaining why — bundling a privilege-escalation-shaped permission
into a general-purpose role (e.g. folding it into `MANAGE_ORGANIZATION_USERS`) would
make escalation implicit and unreviewable at grant time. Anyone who should
be able to manage a tier's authorizations gets that ability as its own
explicit, visible grant, never as a side effect of getting some other role.

**Org-tier and location-tier bootstrap are both solved.**
`OrganizationUserService.registerFounder` auto-grants `MANAGE_ORGANIZATION_ACCESS`
**and** `VIEW_USERS_OF_ORG` to the founder as part of founding — the *only*
place in this codebase a user gets an authorization grant without someone
else already holding the permission required to grant it. `VIEW_USERS_OF_ORG`
is included alongside `MANAGE_ORGANIZATION_ACCESS` because a founder who can grant
roles/permissions but can't see the organization's user list has no way to
find who to grant them to — `MANAGE_ORGANIZATION_ACCESS` alone would leave founding
in a dead end. This is a deliberate, narrow, one-time exception for the
founder specifically, not a precedent for granting things automatically
elsewhere; every other membership creation path (`admitJoinRequests`,
`LocationUserService.createLocationUsers`) still creates a membership row
with zero roles/permissions, exactly as prompt3 originally specified.

`LocationUserService.registerFounder` mirrors the org side: `LocationService.createLocation`
(`organizations/business/location/api/`) calls `SessionContextProvider.initLocation` right
after the new location's schema and row are created — same pattern `OrganizationService.createOrganization`
uses to switch into the freshly created org schema before calling `organizationUserService.registerFounder`
— then calls `locationUserService.registerFounder(locationDto.createdById)`. That creates a
`location_user` membership for whoever created the location (today gated by
`@rtsPermissions.isOrganizationAdmin()` on `LocationEndpoint.createLocation` — a legacy
admin-history check, not an `RtsPermission`/`RtsRole` grant) and grants them
`MANAGE_LOCATION_ACCESS` as an add-on permission, the same way the org founder gets
`MANAGE_ORGANIZATION_ACCESS`. Unlike the org founder, the location founder is **not** also
auto-granted a view permission today — `MANAGE_LOCATION_ACCESS` alone is enough to reach the
location-tier assignment endpoints (§8) and grant further location permissions, but a
location founder with no other org-tier role/permission still can't see the location's user
list (`VIEW_USERS_OF_LOCATION`) to know who to grant them to, the same dead end §7 solved for
the org founder with `VIEW_USERS_OF_ORG`. This is a known asymmetry, not an oversight to
close ad hoc — revisit deliberately (mirroring §7's `VIEW_USERS_OF_ORG` grant) if/when
location self-administration onboarding needs the founder to see who to grant to.

`LocationUserService.createLocationUsers` — the ordinary "add users to an existing location"
path, distinct from `registerFounder` — still creates membership rows with zero
roles/permissions, exactly as before; only the one-time founder moment auto-grants anything.

## 8. REST surface: `AuthorizationAssignmentEndpoint` / `AuthorityCatalogEndpoint`

**Writes** are one request shape, both grant types, split across six
routes on one endpoint class: `AuthorizationAssignmentEndpoint` takes
`AuthorizationAssignmentRequest(userIds, roles = [], permissions = [])`
— every user in `userIds` receives every role *and* every permission
listed (cross product on both). Its `init` block rejects an empty
`userIds` or a request with both lists empty. `allTiers` is
`roles.map { it.tier } + permissions.map { it.tier }` combined — the
whole batch, roles and permissions together, must share one tier.

The six routes are `POST`/`DELETE secured/authorizations/{platform,
organization,location}` — one pair per tier, each with its own static
`@PreAuthorize` (`hasRole(PLATFORM_ADMIN)` / `hasAuthority(MANAGE_ORGANIZATION_ACCESS)`
/ `hasAuthority(MANAGE_LOCATION_ACCESS)`). **Authorization and request
validation are deliberately kept apart**: the annotation is the only
thing that decides who may call a route; `AuthorizationRequestService`
underneath only validates the request's *shape* — `requireExactTier`
(the batch must actually match the tier that route's name and
`@PreAuthorize` promise) and `requireTierContext` (org/location tier
needs its header already resolved) — never a caller-authority check.
This is a general rule, not specific to this endpoint: see
`.claude/instructions.md`'s "Authorization" section — if a single
method would need a dynamic, data-dependent authorization check,
split it into multiple routes instead of writing the check in code.
An earlier version of this endpoint did exactly that (one route for
all three tiers, an imperative `requireCallerCanManage` inside the
service); it was reverted for this reason, not for a technical one —
see §3/§7 elsewhere in this doc for the same "prefer eliminating a
dynamic check by construction" reasoning applied to org/location's
tier-access gate.

**Why the transactional batch logic still lives on a separate bean,
`AuthorizationBatchExecutor`, rather than on `AuthorizationRequestService`
itself**: Spring's transactional proxying does not apply on
self-invocation — a `@Transactional`-style method called via `this`
from another method on the *same* bean bypasses the proxy and silently
runs with no transaction at all. `AuthorizationRequestService`'s six
methods each need to call *into* a transactional method after
validating; putting that transactional method on the same class would
have been exactly that bug, regardless of how many routes call in.
`AuthorizationBatchExecutor` exists purely so the call crosses a real
bean boundary: it has six methods
(`assign`/`removeAt{Platform,Organization,Location}Tier`), each
`@TransactionalOn<Tier>Schema`, each looping the batch calling both
`RoleGrantService` and `PermissionGrantService` for every user — one
transaction spanning both grant types, because by the time this runs
the request has already been confirmed single-tier by the route it
came in on, so it's one datasource regardless of how many
roles/permissions or users are in it.

**This went through several shapes before landing here** — worth
knowing if you're tempted to re-shape it again:
1. *Twelve methods, one per tier x type x (assign|remove)*, across
   two endpoints — correct, but meant a role batch and a permission
   batch for the same users could never share a transaction, and
   callers needing both had to make two requests.
2. *One endpoint, one route, all tiers and both types combined,
   authorization checked imperatively* — solved the "share a
   transaction" problem via `AuthorizationBatchExecutor`, but
   reintroduced a dynamic authorization check the codebase's own
   convention says to avoid.
3. **This shape** — one request combining both grant types (keeping
   §2's transaction fix), six routes split by tier so
   `@PreAuthorize` stays static on each (keeping §1's declarative
   authorization). Getting both properties at once needed both
   pieces: the separate executor bean for the transaction, and the
   per-tier route split for the authorization.

**Reads** stay on their own endpoint, `AuthorityCatalogEndpoint`
(`secured/authorities`) — a read and a write are different concerns
even when they'd reuse the same tier-access gate, so this was never
folded into the endpoint above. Four of its five methods go through
`AuthorizationCatalogService`; the fifth (the platform-only holder
lookup, below) goes through a separate platform-tier service instead:

- `GET secured/authorities/per-role?role=X` — **open to any
  authenticated user**, no `@PreAuthorize` at all. Returns a
  `List<PermissionResponse>` (`permission: RtsPermission`,
  `role: RtsRole`) — one entry per permission that role bundles, each
  tagged with the role so a UI calling this per-role over time can
  accumulate and group a combined dataset without losing which role
  each permission came from. Knowing what a named role bundles isn't
  privileged the way granting/revoking or seeing a tier's catalog is,
  so this is deliberately the only ungated method in the class — a
  controller mixing gated and ungated methods is fine; each method's
  own annotation (or absence of one) is what's authoritative, not the
  class it happens to live in.
- `GET secured/authorities/platform` — `hasRole(PLATFORM_ADMIN)`.
- `GET secured/authorities/organization` — `hasAuthority(MANAGE_ORGANIZATION_ACCESS)`.
- `GET secured/authorities/location` — `hasAuthority(MANAGE_LOCATION_ACCESS)`.

All three are symmetric — each returns **only its own tier's**
roles/permissions via `AuthorizationCatalogService.getCatalogForTier`,
never a cross-tier "full" list (there used to be a separate
`getFullCatalog` for the platform route; it was removed once platform
tier was made to follow the same one-tier-per-call rule as the other
two — nothing needs the union of all three tiers in one response, and
carrying that special case around would have been the one asymmetry
in an otherwise uniform design). Each returns a flat `List<Authority>`
(`name`, `tier`, `type` — `ROLE` or `PERMISSION` — and `holderCount`;
no per-role bundle detail — that's what the `per-role` lookup above is
for). `holderCount` is only ever non-null for `PLATFORM`-tier
entries — org/location tiers have no `getHolders` (see below), so
their `Authority` rows always carry `holderCount = null` rather than a
fabricated or always-zero count. A dedicated `PlatformRoleEndpoint`
(`GET secured/platform-roles`) used to expose the same per-role counts
as a separate `PlatformRoleSummary` (`role`, `holderCount`) response;
it was retired once `Authority` grew a real `holderCount`, since
serving the identical count through two different response shapes on
two different controllers had no reason to keep existing.

A fifth method, `GET secured/authorities/platform/holders?authorityName=X&authorityType=Y`
(`hasRole(PLATFORM_ADMIN)`), is the reverse lookup — not "what does
this role bundle" but "who currently holds this specific role or
permission," platform tier only. It's backed by
`PlatformAuthorityHolderService.getHolders`
(`platform/business/authority/api/`), a class of its own rather than a
method on `AuthorizationCatalogService`: everything else on that
service (`getCatalogForTier`, `getPermissionsForRole`) is genuinely
tier-agnostic — the same code path serves all three tiers — while this
lookup is platform-only by nature (org/location have no equivalent),
so it doesn't belong in the shared, cross-tier class any more than
`PlatformRoleAssignmentService` itself does. `authorityName`/`authorityType`
mirror `Authority.name`/`Authority.type` — the same two fields the
platform catalog (`getPlatformCatalog`) already returns per row — so a
caller can go straight from a row in that list to this lookup without
reshaping anything. Resolving the pair into an actual `RtsRole` or
`RtsPermission` (by `valueOf`, rejecting an unknown name or one that
isn't `PLATFORM`-tier) happens in `PlatformAuthorityLookup.of`, a
private sealed interface (`ByRole`/`ByPermission`) living in the same
file as `PlatformAuthorityHolderService`, that turns "exactly one of
two possible things, resolved from a name" into a type the `when` in
`getHolders` can exhaust — no null-checking, no `!!`. The method
returns one `AuthorityHolderResponse` (`fullName`, `grantedOn`,
`authorityName`) per row in
`platform_role_assignment`/`platform_permission_assignment` matching
it. `fullName` reuses `UserQualifier.getUserFullName` — the same bean
`OrganizationUserMapper` already uses as a MapStruct qualifier —
rather than re-deriving first-name/last-name joining logic here.

There used to be a role-only counterpart to this, moved over from the
retired `PlatformRoleEndpoint`
(`GET secured/authorities/platform/roles/{role}/holders`,
`PlatformRoleHolder`); it was dropped rather than kept, since it
returned the exact same underlying grants as this fifth method with
only a different response shape (`userId`/`email` instead of
`authorityName`) — not enough of a reason for two endpoints to answer
the same "who holds this role" question.

The underlying `getHolders(role|permission)` methods on
`PlatformRoleAssignmentService`/`PlatformPermissionAssignmentService`
(distinct from `PlatformAuthorityHolderService.getHolders` above — same
verb, different layer) return `AuthorityGrant` (`userId`, `grantedAt`),
a small shared DTO living in this package (`cross_tier/authority/`) —
both platform's `role_assignment/api` and `permission_assignment/api`
importing it is a normal dependency on tier-agnostic shared code, not
a violation.
They also back `holderCountOf` (§ above) for the platform-tier catalog.
Org/location tiers have no equivalent `getHolders`; that's deliberate,
not a gap to fill — org/location membership and role data is already
available via `UserAccessDetail`, so a holder-count/lookup
feature at those tiers has no concrete need driving it yet. Don't
build it ahead of that need.
