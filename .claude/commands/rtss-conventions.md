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

## YAML

- Two-space indentation. Fix any deviation found while editing.

## READMEs

- Package READMEs are doctrine. When renaming or refactoring anything in a package that has a README, update the README in the same change.
- Never leave stale class/method/field names behind in a README.

## Source of truth

Full standards: `.claude/instructions.md`
