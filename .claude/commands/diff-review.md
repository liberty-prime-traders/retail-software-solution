Perform a deep code review of the current branch changes.

1. Load project context by invoking the `/rtss-conventions` and `/rtss-architecture` skills. Use their rules as the authoritative standard for project-specific findings below.
2. Run `git diff HEAD~1` (or `git diff main...HEAD` if on a feature branch) to get the full diff.
3. Analyze the diff across these five dimensions:
   - **Logic Gaps** — incorrect assumptions, missing edge cases, wrong ordering, race conditions, data integrity issues
   - **Performance** — N+1 queries, unbounded loops, unnecessary full-table loads, missing indexes for query patterns, lock contention
   - **Clean Code** — redundant try/catch that only rethrows, duplicated business logic, misleading names, dead fields
   - **Architecture** — cross-domain boundary violations (per ArchitectureTest rules), wrong layer placement, tight coupling between layers that should communicate via api packages only
   - **Anti-patterns** — denormalized running totals without proper concurrency control, empty consumers with TODOs in production paths, overly specific service names, platform-specific SQL types in a project targeting a different DB
   - **RTSS Conventions & Architecture** — violations of the rules loaded from `/rtss-conventions` and `/rtss-architecture`: abbreviated names, param names that don't match their type, star imports, fat endpoints, LocalDate.now() / ZoneOffset.UTC usage, cross-domain imports outside `api/`, Kafka processors missing `shouldProcess` or idempotency constraint, UpdateDto misuse, MapStruct O(n) mapper calls, stale READMEs

4. For each finding, include:
   - The specific file path and line number
   - A concise description of the problem
   - Why it matters (correctness, scalability, maintainability)

5. Save the full report to `./.claude/claudesomemdfile.md`, organized by the five sections above.

Be blunt. Skip findings that are trivially cosmetic.
