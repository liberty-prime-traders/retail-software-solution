# Auth Package — Rules & Expectations

This document is the canonical reference for **how login and identity
linking work** in RTSS. It covers `platform/business/auth` and the
session-token/security-context pieces it depends on
(`configuration/security/SessionTokenService`,
`configuration/filters/TenantFilter`).

Read `platform/business/role_assignment/README.md` first if you haven't
— login's last step (`verifyRequestedRoles`) and every request after
login (`TenantFilter.refreshEffectiveRoles`) both go through
`RoleResolutionService` from that package.

## 1. Scope & Public Surface

```
platform/business/auth/       — identity-provider link + pending-link entities/repositories
platform/business/auth/api/   — public surface (AuthService, LoginRequest/Response, IdentityProviderService)
```

| Class                        | Purpose                                                                                                     |
|------------------------------|-------------------------------------------------------------------------------------------------------------|
| `AuthService`                | The whole login flow — resolve identity, mint session token, verify roles                                   |
| `IdentityProviderService`    | One implementation per provider (`supports`/`authenticate`); Google is the only one wired up                |
| `IdentityProviderLinkEntity` | Permanent `(provider, external_id) -> user_id` link, one row per confirmed link                             |
| `PendingIdentityLinkEntity`  | A proposed link awaiting out-of-band confirmation (see §3)                                                  |
| `SessionTokenService`        | Mints the HS256 session JWT (`sub` = system user id, org/location come from request headers, not the token) |
| `TenantFilter`               | Per-request: resolves org/location schema, then refreshes authorities (§4)                                  |

## 2. How `login()` resolves a user

`AuthService.resolveSystemUserId` tries three things, in order:

1. **Known link** — `identity_provider_link` already has a row for this
   `(provider, externalId)`. Use its `userId`. This is the common case
   for every login after the first.
2. **No link, but the email matches an existing `sys_user`** — do
   **not** silently attach the new provider to that account. Instead:
   invalidate any other `PENDING` links for that user+provider
   (`invalidateExistingPendingLinks`) and create a new
   `PendingIdentityLinkEntity`, then throw
   `AuthException.ProviderMismatch(pendingLinkToken)`. The caller gets
   a 401 carrying that token. **There is currently no endpoint that
   consumes a pending link** — `PendingLinkStatus.CONSUMED` and
   `consumedAt` exist on the entity for that future flow, but nothing
   sets them yet. Don't assume a consume endpoint exists just because
   the entity has the columns for it.
3. **No link, no matching email** — create a brand-new `sys_user` and
   link it immediately (`linkIdentity`). No pending state involved;
   this is a first-ever signup, not an account-takeover risk.

The reason step 2 doesn't just auto-link: an unverified claim that
"this Google account's email matches an existing user" is not proof of
ownership of that existing account. Auto-linking on email match alone
would let anyone with a throwaway Google account claim any user's
account, so long as they controlled that same email address elsewhere
too (or the email happened to collide). The pending-link token exists
so a *second* factor — some confirmation step tied to the existing
account — can approve the merge instead.

## 3. `rolesToVerify` on login is platform-tier only, by design

`AuthService.verifyRequestedRoles` filters `loginRequest.rolesToVerify`
down to names that (a) parse as a real `RtsRole`, (b) are
`SchemaLevel.PLATFORM`, and (c) the user actually holds — silently
dropping everything else, including a syntactically valid but
unrecognized name and a real role the user simply doesn't have. This
is deliberate, not an oversight: distinguishing "not a real role name"
from "a real role you don't hold" in the response would let an
unauthenticated caller (this runs *before* any session exists) probe
for which role names are valid.

Org/location roles are excluded here because `login()` runs on the
platform datasource before any org/location schema has been resolved —
there is no schema context yet to check an org/location role against.
`TenantFilter.refreshEffectiveRoles` (next section) is what picks up
org/location roles, once a request actually carries org/location
headers.

## 4. Session JWT vs. effective authorities — two different lifetimes

The JWT `SessionTokenService.mint()` returns is deliberately thin: just
`sub` (system user id) and standard timing claims. It carries **no**
roles and is **not** re-minted per request.

What backs `hasRole(...)` checks is entirely separate and lives only in
memory for the duration of one request: `TenantFilter` calls
`RoleResolutionService.getEffectiveRoles(systemUserId)` on every
request (after org/location schema resolution, so org/location roles
are visible if that context exists) and replaces
`SecurityContextHolder`'s authentication with a new
`JwtAuthenticationToken` carrying `ROLE_${role.name}` authorities built
from that fresh lookup. This means:

- A role granted or revoked mid-session takes effect on the **next
  request**, not the current one and not by re-authenticating — the
  JWT itself never needed to change.
- If `SessionContextProvider.getUserIdOrNull()` is null, or the current
  principal isn't a `Jwt` (e.g. the request never authenticated),
  `refreshEffectiveRoles` returns early and leaves whatever authorities
  were already on the context — there is no "default deny" rewrite for
  an unauthenticated request; unauthenticated requests to secured
  routes are rejected upstream, before this filter's authority swap
  would matter.
