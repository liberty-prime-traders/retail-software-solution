# Role Assignment (Platform Tier)

This package holds only the platform-tier persistence for role
grants: `PlatformRoleAssignmentEntity`, `PlatformRoleAssignmentRepository`,
`PlatformRoleCache` (per-schema Caffeine cache, keyed by userId, fully
invalidated on any write), and `api/PlatformRoleAssignmentService`
(`getRoles`/`holds`/`getHolders`/`assign`/`remove`) — the same shape
`organizations/business/role_assignment/` and
`locations/business/role_assignment/` each have for their own tier.

Everything tier-agnostic that used to live alongside this — the
`Authority`/`GrantedRole`/`GrantedPermission`/membership DTOs and the
orchestrator services (`EffectiveAuthorizationService`,
`RoleGrantService`, `PermissionGrantService`,
`AuthorizationRequestService`, `AuthorizationBatchExecutor`,
`AuthorizationCatalogService`) — moved to `cross_tier/authority/`,
since none of it is specific to the platform tier: it's used
identically by all three tiers, and `cross_tier/` is where code with no
single tier owner belongs (outside any `<tier>/business/<domain>`
package, so `ArchitectureTest`'s domain-isolation rule doesn't apply
to it — see that package's README for the reasoning and the full
authorization design).

`api/PlatformRoleAssignmentService` stays here rather than moving too,
because it's still tier-specific: it's the one concrete
persistence-backed service `cross_tier/authority/` depends on for the
platform tier, exactly mirroring `OrgRoleAssignmentService`/
`LocationRoleAssignmentService` in the other two tiers.
