# RTSS — Coding Conventions

Apply these rules to every file touched in this session.

## Naming

- Variable and param names are **verbose** — never abbreviate. `purchasePaymentCeilingService`, not `ceilingService`.
- **Param name must match its type**: `saleSaveRequest: SaleSaveRequest`, never `input`, `data`, `dto`, or `result`.
- When applying a naming fix, actively hunt for the same pattern in surrounding files — don't only fix what was cited.

## Imports

- Never star imports.
- Use import statements everywhere. FQNs only when there is an actual name conflict in the same file.

## Comments

- Comments call out **gotchas only** — a hidden constraint, a workaround, a subtle invariant.
- Never explain what the code already says. If removing the comment wouldn't confuse a future reader, don't write it.
- No multi-line comment blocks or docstrings.

## Endpoints

- Endpoints are **thin delegators**. No mapping, projection, or assembler calls in the endpoint body.
- All that logic lives in services or fetchers.

## Authorization

- Keep authorization (`@PreAuthorize`) separate from request validation — never check caller permissions inside a service method that also validates the request's shape.
- If one endpoint method would need a dynamic/data-dependent authorization check, split it into multiple routes instead, so each route's `@PreAuthorize` stays a static expression.

## YAML

- Two-space indentation. Fix any deviation found while editing.

## READMEs

- Package READMEs are doctrine. When renaming or refactoring anything in a package that has a README, update the README in the same change.
- Never leave stale class/method/field names behind in a README.
- Write in **canonical, timeless voice** — describe how the system behaves, not what was just done to it. A README is read long after the change that prompted it; it must read the same on day one and year three.
  - Never reference the current task, request, session, "this diff", "this change", or what was or wasn't asked for. That belongs in the commit message or PR description, not the doctrine.
  - Banned: "Not fixed here because it wasn't asked for", "Fixed as part of this change", "Per the user's request, this now...", "As requested,", "Status update:", "I added/changed/removed...".
  - Known gaps and deliberate non-fixes still belong in the doc, but state them as a property of the system: what's true, why it's true, and what would justify revisiting it — never as a note about the work session. Prefer the existing doc's own voice, e.g. "This is a known asymmetry, not an oversight to close ad hoc — revisit deliberately if/when X needs it," over "Not fixed here because it wasn't asked for."

## Parallel lists must stay in sync — enforce it, don't rely on memory

Whenever two independent lists are supposed to describe the same set of things (e.g. `TableNames`
constants vs. `registry-entries/` seed files), add a test that diffs them in both directions
(missing + orphaned) instead of trusting the next person to remember to update both. If a missing
entry is silent at runtime (no exception on the code path that would use it), also add a
`StartupCheck` (see `startup_checks/api/`) so a real deployment fails fast instead of shipping a
gap that's only found by accident. See `rtss-db`'s "Registry completeness" section for the
concrete instance of this pattern.

## Source of truth

Full standards: `.claude/instructions.md`
