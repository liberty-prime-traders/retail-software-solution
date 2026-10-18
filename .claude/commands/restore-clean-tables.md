# RTSS — Update restore-clean table lists

Keep `scripts/clean-restored-db.sql` and `scripts/restore-db-shells-only.md` in sync with the Liquibase changelogs. Run after any table or denormalized column is added, renamed, or dropped.

## Steps

1. List current tables: `src/main/resources/db/changelog/{organizations,locations}/tables/*.yml` (and `*/audit/tables/*_aud.yml`).
2. Diff against `organization_tables_to_truncate` and `location_tables_to_truncate` in `scripts/clean-restored-db.sql`, plus the "kept" lists in the doc.
3. Classify every table not yet in the script:
   - **Truncate**: transactional data (sales, purchases, stock, ledgers, subledgers, expenses, event logs) and its `_aud` table.
   - **Keep**: shell/basic data (products, contacts, accounts, units, tax rates, payment methods, users, roles, permissions, catalog, config) and its `_aud` table.
   - Unsure: ask the user, do not guess.
4. For each new column that caches or denormalizes a value (balances, running totals, last-price, closed-at style flags), add a matching `UPDATE` reset in the script. Look for `current_balance`, `running_*`, `last_*`, `total_*`.
5. Drop entries for tables that no longer exist. The script skips missing tables, but stale names hide drift.
6. Check each truncated table's sequence is `seq_<table>_reference_number`; if a table uses a different sequence name, handle it explicitly in the script.
7. Confirm no kept table has an FK into a truncated table (the script has no `CASCADE` on purpose). If one does, either truncate it too or reclassify.
8. Update the tables and prose in `scripts/restore-db-shells-only.md` in the same turn so it matches the script. The script must still end in `ROLLBACK;`.

## Rules

- Never add `CASCADE` to the `TRUNCATE`.
- Never touch the `platform` schema.
- Report what was added, removed, and reclassified, and anything left undecided.
