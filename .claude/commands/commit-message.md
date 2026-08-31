Generate a commit message for the current changes and save it — do not run `git commit`.

1. Determine the JIRA issue number from the current branch name: run `git branch --show-current` and extract the leading ticket id (pattern `[A-Z]+-[0-9]+`, e.g. `RTSS-206` from `RTSS-206-Rename-variables-...`). If the branch name has no leading ticket id, ask the user for one instead of guessing.
2. Run `git status` and `git diff` (staged changes if any are staged, otherwise unstaged) to see what actually changed. Also check `git log -5 --format='%s'` on this branch for phrasing precedent, but the shape below overrides it.
3. Compose the message in this shape:
   - **Line 1**: `<JIRA-ID> <brief imperative summary>` — one line, no period, describes the overall change (e.g. `RTSS-206 rename productId to orgProductId/locationProductId codebase-wide`).
   - Blank line.
   - **1–3 short paragraphs** of plain prose (not a bullet list per file) covering what changed and, more importantly, *why* — the motivation, the bug being fixed, or the ambiguity being resolved. Each paragraph is a few sentences. Total body stays well under a page — a handful of short paragraphs, not an exhaustive changelog.
4. Write the full message (summary line + blank line + paragraphs) to `./.claude-local/commit-message.md`, overwriting any existing content.
5. Don't bother printing the generated message back to the user. You can remind them it's saved to `.claude-local/commit-message.md` for review before they commit — this command does not stage or commit anything.
