# RTSS — Database Patterns

Apply these rules to every Liquibase, entity, or repository change.

## Changeset management (pre-live)

- **Edit the original changeset in-place** for drops (index, constraint, column). Never add a separate drop changeset.
- Each changeset gets label `1.0.0`.
- Blank lines between changesets and between column definitions.

## Constraint naming

- Unique constraints: `uq_<tblname>_<colname>`
- Foreign keys: `fk_<tblname>_<colname>`
- Not-null on table create: `<tblname>_<colname>_notnull`
- Full pattern: `tblname_colname_constrainttype`

## Changelog load order (within each layer)

```
tables/ → sequences/ → foreign_keys/ → triggers/ → indexes/ → audit/
```
Platform layer adds after audit: `version-entries/ → registry-entries/`

## New tables

1. Add sequence for the reference number column.
2. Reference number = prefix (from registry) + zero-padded number to ≥6 chars total (e.g. `OR000042`).
3. Register in `registry-entries/` and in `TableNames` constants.
4. Add audit table + register it in `TableNames` + registry — unless the table is append-only or
   otherwise deliberately not `@Audited` (see "What NOT to audit"), in which case there is no
   audit table and nothing to register for it.
5. Audit table indexes: one on `rev`, one on `(id, rev)`.
6. Create sequences, triggers, extensions, FKs, indexes in their own subfolders.

## `TableNames` / `TableName` ordering

Both files group entries under the same comment headers (`Platform tables`, `Organization tables`,
`Location tables`, and their `*audit tables` counterparts). Within each section, keep entries
alphabetical by constant/enum name — insert a new entry in sorted position, don't just append it
to the end of the section. Don't reorder across sections or move an entry to a "more correct"
section to fix a pre-existing miscategorization (e.g. a table's `SchemaLevel` disagreeing with
which comment block it sits under) — that's a separate, deliberate change, not a byproduct of
alphabetizing.

## Entity / JPA rules

- No migration info on entities — column names and insert/update restrictions only.
- Use `TableNames` constants for all table name strings.
- All tables get `created_by_id` and `created_on` auditing columns.
- No `@Enumerated`. Use a converter class. Column size defaults to 5 characters.
- Envers `@Audited` on mutable domain entities. Do NOT audit: lookup tables, `created_on`, `created_by_id`, `reference_number`.

## What NOT to audit

Lookup/reference tables and immutable columns (`created_on`, `created_by_id`, `reference_number`).
