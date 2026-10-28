# Restore DB as "shells + basic data" only

## Approach

`scripts/restore-db.sh` copies the whole database (`pg_dump -Fc | pg_restore`). It has no selective mode, so keep it as is:

1. Run `restore-db.sh <src> <dst>` as usual.
2. Run `scripts/clean-restored-db.sql` **against the target DB only** (never the source).

The script is one transaction. It discovers every `org_*` and `loc_*` schema itself, so it works for any number of orgs and locations. `platform` is not touched.

## What is kept vs. cleared

### Organization schemas (`org_*`)

| Action                        | Tables                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Truncated**                 | `ledger_entry`, `ledger_entry_group`, `subledger_entry`, `subledger_balance`, `opening_balance`, `contact_opening_balance`, `org_expense`, `org_expense_batch`, `org_expense_payment`, `org_expense_payment_state`, `org_expense_payment_void`, `org_expense_void`, `fund_movement`, `fund_movement_void`, `loan`, `loan_repayment`, `loan_repayment_void`, `loan_void`, `stock_transfer_order` (+ `_aud`), `event_processing_log` |
| **Denormalized values reset** | `account.current_balance = 0`, `account.balance_updated_at = NULL`; `fiscal_period.closed_at / closed_by = NULL` (periods were closed over ledger data that no longer exists)                                                                                                                                                                                                                                                      |
| **Kept as is**                | everything else: accounts, contacts, products, categories, groups, tags, units, tax rates, payment methods, locations, users/roles/permissions, expense types, adjustment reasons, accounting config, fiscal periods, `*_aud` of kept tables, `revinfo`                                                                                                                                                                            |

### Location schemas (`loc_*`)

| Action                        | Tables                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Truncated**                 | `sale`, `sale_line`, `sale_adjustment`, `sale_line_stock_reservation`, `sale_payment`, `sale_payment_void`, `sale_void`, `purchase`, `purchase_line`, `purchase_delivery`, `purchase_delivery_line`, `supplier_payment`, `supplier_payment_void`, `supplier_return`, `stock_entry`, `stock_movement`, `stock_adjustment`, `opening_stock`, `stock_transfer_*` (dispatch, dispatch_line, draft_line, receipt, receipt_line), `expense*`, `tax_entry`, plus the matching `*_aud` tables |
| **Denormalized values reset** | `location_product.last_purchase_price = NULL`                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| **Kept as is**                | `location_product` (the per-location catalog and prices), `location_user`, `location_role_assignment`, `location_permission_assignment`, `sync_log`, their `_aud` tables, `revinfo`                                                                                                                                                                                                                                                                                                   |

If you want the per-location catalog gone too, add `location_product` (and `location_product_aud`) to `location_tables_to_truncate`. Note that the location product ids are what the sale/session flows hang off, so you would need to re-sync it from the org products afterwards.

### Sequences

The `seq_<table>_reference_number` sequence of every truncated table is restarted at 1. Sequences of kept tables (contacts, products, ...) keep counting, so existing reference numbers stay unique.

## Design notes

- **No `CASCADE`.** All tables go into one `TRUNCATE` statement. If a kept table has an FK into a truncated one, Postgres raises an error and the transaction rolls back, instead of silently wiping the kept table. If that happens, the error names the table: decide whether it belongs in the list.
- **Missing tables are skipped** (`to_regclass`), so the script survives changelog drift between environments.
- **Dry run first**: leave the final `ROLLBACK;` in place, check the `NOTICE` output and the verification result, then swap it for `COMMIT;`.
- **Not touched**: Kafka topics/consumer offsets and Redis. After the restore, stale Kafka events for removed sales/purchases can still arrive. Use a fresh consumer group (or reset the topics) in the target stack, and `FLUSHALL` the target Redis if it caches anything per org.
- Run with the target's `rtss-server` stopped (the restore script already does that when the server runs in Docker; restart it afterwards).

## Script

`scripts/clean-restored-db.sql` is the single source of truth for the table lists and resets; this doc describes what it does and does not repeat it. Run it against the **target**:

```bash
docker exec -i <target-postgres-container> psql -U <user> -d <db> -v ON_ERROR_STOP=1 < scripts/clean-restored-db.sql
```

## After committing

1. Restart `rtss-server` (the app's context tests/dev DB expect Liquibase to be current, so run it on the target as usual).
2. Spot-check: `SELECT count(*) FROM org_liberty.account WHERE current_balance <> 0;` should return 0.
3. Re-enter opening stock/balances through the normal flows; they will post fresh ledger and subledger rows.

## Alternative (if the restore itself is slow)

`pg_dump --exclude-table-data='loc_*.sale*'` style flags skip the data at dump time, but `--exclude-table-data` takes patterns per table and cannot reset `account.current_balance`, so you would still need the `UPDATE` statements. The post-restore script is simpler and less error-prone.
