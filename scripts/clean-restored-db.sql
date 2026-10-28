BEGIN;

DO $cleanup$
DECLARE
  organization_tables_to_truncate text[] := ARRAY[
    'ledger_entry', 'ledger_entry_group',
    'subledger_entry', 'subledger_balance',
    'opening_balance', 'contact_opening_balance',
    'org_expense', 'org_expense_batch', 'org_expense_payment',
    'org_expense_payment_state', 'org_expense_payment_void', 'org_expense_void',
    'fund_movement', 'fund_movement_void',
    'loan', 'loan_repayment', 'loan_repayment_void', 'loan_void',
    'stock_transfer_order', 'stock_transfer_order_aud',
    'event_processing_log'
  ];

  location_tables_to_truncate text[] := ARRAY[
    'sale', 'sale_aud', 'sale_line', 'sale_line_aud', 'sale_adjustment', 'sale_adjustment_aud',
    'sale_line_stock_reservation', 'sale_payment', 'sale_payment_void', 'sale_void',
    'purchase', 'purchase_aud', 'purchase_line', 'purchase_line_aud',
    'purchase_delivery', 'purchase_delivery_aud', 'purchase_delivery_line', 'purchase_delivery_line_aud',
    'supplier_payment', 'supplier_payment_void', 'supplier_return', 'supplier_return_aud',
    'stock_entry', 'stock_entry_aud', 'stock_movement', 'stock_movement_aud',
    'stock_adjustment', 'opening_stock',
    'stock_transfer_dispatch', 'stock_transfer_dispatch_aud',
    'stock_transfer_dispatch_line', 'stock_transfer_dispatch_line_aud',
    'stock_transfer_draft_line', 'stock_transfer_draft_line_aud',
    'stock_transfer_receipt', 'stock_transfer_receipt_aud',
    'stock_transfer_receipt_line', 'stock_transfer_receipt_line_aud',
    'expense', 'expense_batch', 'expense_payment', 'expense_payment_state',
    'expense_payment_void', 'expense_void',
    'tax_entry', 'tax_entry_aud'
  ];

  organization_schema_name text;
  location_schema_name text;
  qualified_table_names text;
  sequence_name_to_restart text;
BEGIN
  FOR organization_schema_name IN
    SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'org\_%' ORDER BY schema_name
  LOOP
    SELECT string_agg(format('%I.%I', organization_schema_name, table_name), ', ')
      INTO qualified_table_names
      FROM unnest(organization_tables_to_truncate) AS table_name
     WHERE to_regclass(format('%I.%I', organization_schema_name, table_name)) IS NOT NULL;

    IF qualified_table_names IS NOT NULL THEN
      EXECUTE 'TRUNCATE TABLE ' || qualified_table_names;
    END IF;

    FOR sequence_name_to_restart IN
      SELECT sequencename FROM pg_sequences
       WHERE schemaname = organization_schema_name
         AND sequencename IN (SELECT 'seq_' || table_name || '_reference_number' FROM unnest(organization_tables_to_truncate) AS table_name)
    LOOP
      EXECUTE format('ALTER SEQUENCE %I.%I RESTART', organization_schema_name, sequence_name_to_restart);
    END LOOP;

    IF to_regclass(format('%I.account', organization_schema_name)) IS NOT NULL THEN
      EXECUTE format('UPDATE %I.account SET current_balance = 0, balance_updated_at = NULL', organization_schema_name);
    END IF;
    IF to_regclass(format('%I.fiscal_period', organization_schema_name)) IS NOT NULL THEN
      EXECUTE format('UPDATE %I.fiscal_period SET closed_at = NULL, closed_by = NULL', organization_schema_name);
    END IF;

    RAISE NOTICE 'cleaned organization schema %', organization_schema_name;
  END LOOP;

  FOR location_schema_name IN
    SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'loc\_%' ORDER BY schema_name
  LOOP
    SELECT string_agg(format('%I.%I', location_schema_name, table_name), ', ')
      INTO qualified_table_names
      FROM unnest(location_tables_to_truncate) AS table_name
     WHERE to_regclass(format('%I.%I', location_schema_name, table_name)) IS NOT NULL;

    IF qualified_table_names IS NOT NULL THEN
      EXECUTE 'TRUNCATE TABLE ' || qualified_table_names;
    END IF;

    FOR sequence_name_to_restart IN
      SELECT sequencename FROM pg_sequences
       WHERE schemaname = location_schema_name
         AND sequencename IN (SELECT 'seq_' || table_name || '_reference_number' FROM unnest(location_tables_to_truncate) AS table_name)
    LOOP
      EXECUTE format('ALTER SEQUENCE %I.%I RESTART', location_schema_name, sequence_name_to_restart);
    END LOOP;

    IF to_regclass(format('%I.location_product', location_schema_name)) IS NOT NULL THEN
      EXECUTE format('UPDATE %I.location_product SET last_purchase_price = NULL', location_schema_name);
    END IF;

    RAISE NOTICE 'cleaned location schema %', location_schema_name;
  END LOOP;
END
$cleanup$;

-- Verification: row_count should be 0 for every row below.
SELECT table_schema AS schema_name, table_name,
       (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), false, true, '')))[1]::text::bigint AS row_count
  FROM information_schema.tables
 WHERE (table_schema LIKE 'org\_%' AND table_name IN ('ledger_entry', 'subledger_entry', 'org_expense', 'fund_movement', 'loan'))
    OR (table_schema LIKE 'loc\_%' AND table_name IN ('sale', 'purchase', 'stock_movement', 'stock_entry', 'stock_adjustment'))
 ORDER BY table_schema, table_name;

ROLLBACK;  -- dry run; change to COMMIT; once the output looks right
