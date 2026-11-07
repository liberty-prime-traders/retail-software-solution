Generate SQL to retain data, strategically delete from databasechangelog per relevant schema, and reapply the changeset with in-place edits. Use it when a changeset that has not reached a live environment was edited in place and a dev database already ran the old version.

Use the appropriate diff to see what changelog will fail and write statements for them. For example, a column edit on a table
will likely generate `create table x_old as x; drop table x; user executes liquibase ...; insert into x select * from x_old; drop table x_old;` to retain data.
Note that you do not write sql to migrate the schema. We already have an enpoint for that. You just need to clean DDL
and changelogs, as well as restoring (with defaults if needed) after user says endpoint worked.

When a table is dropped, also delete the databasechangelog rows of its sibling changesets (indexes, foreign keys, sequences, audit),
or they are skipped on the rerun and the recreated table comes back without them.

Use exact changelog ids. Brand-new changelogs are safe and need no action.

Ask for clarify where to diff with if you can't see the relevant changes. this is also schema based.

Your output is `@.claude-local/migration-plan`
