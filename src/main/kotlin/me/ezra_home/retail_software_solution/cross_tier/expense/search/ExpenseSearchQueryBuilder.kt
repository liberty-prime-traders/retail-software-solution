package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.ExpressionRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object ExpenseSearchQueryBuilder {

    private const val EXPENSE_ALIAS = ExpenseSearchAliases.EXPENSE
    private const val PAYMENT_STATE_ALIAS = ExpenseSearchAliases.PAYMENT_STATE
    private const val EXPENSE_VOID_ALIAS = ExpenseSearchAliases.EXPENSE_VOID
    private const val EXPENSE_PAYMENT_ALIAS = ExpenseSearchAliases.EXPENSE_PAYMENT
    private const val EXPENSE_PAYMENT_VOID_ALIAS = ExpenseSearchAliases.EXPENSE_PAYMENT_VOID

    private fun fromAndJoins(tables: ExpenseSearchTables) = """
        FROM ${tables.expense} $EXPENSE_ALIAS
        JOIN ${tables.paymentState} $PAYMENT_STATE_ALIAS ON $PAYMENT_STATE_ALIAS.expense_id = $EXPENSE_ALIAS.id
        LEFT JOIN ${tables.expenseVoid} $EXPENSE_VOID_ALIAS ON $EXPENSE_VOID_ALIAS.expense_id = $EXPENSE_ALIAS.id
    """

    private const val IS_VOIDED = "($EXPENSE_VOID_ALIAS.id IS NOT NULL)"

    fun buildPredicate(expenseSearchParameters: ExpenseSearchParameters, tables: ExpenseSearchTables): QueryBuilderContext {
        val context = QueryBuilderContext()
        context.whereClauses.add("1=1")

        DateRangeFilterStrategy(
            "$EXPENSE_ALIAS.created_on",
            ExpenseSearchParameterNames.CREATED_FROM,
            expenseSearchParameters.createdFrom,
            ExpenseSearchParameterNames.CREATED_BEFORE,
            expenseSearchParameters.createdBefore
        ).apply(context)

        DateRangeFilterStrategy(
            "$EXPENSE_ALIAS.expense_date",
            ExpenseSearchParameterNames.EXPENSE_DATE_FROM,
            expenseSearchParameters.expenseDateFrom,
            ExpenseSearchParameterNames.EXPENSE_DATE_BEFORE,
            expenseSearchParameters.expenseDateBefore
        ).apply(context)

        AnyListFilterStrategy(
            "$EXPENSE_ALIAS.payee_contact_id",
            ExpenseSearchParameterNames.PAYEE_CONTACT_IDS,
            expenseSearchParameters.payeeContactIds.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "$EXPENSE_ALIAS.expense_type_id",
            ExpenseSearchParameterNames.EXPENSE_TYPE_IDS,
            expenseSearchParameters.expenseTypeIds.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "$EXPENSE_ALIAS.source_reference",
            ExpenseSearchParameterNames.SOURCE_REFERENCES,
            expenseSearchParameters.sourceReferences.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "$EXPENSE_ALIAS.reference_number",
            ExpenseSearchParameterNames.EXPENSE_REFERENCE_NUMBERS,
            expenseSearchParameters.expenseReferenceNumbers.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "$PAYMENT_STATE_ALIAS.payment_status",
            ExpenseSearchParameterNames.PAYMENT_STATUSES,
            expenseSearchParameters.paymentStatuses.map { it.code }.toTypedArray()
        ).apply(context)

        ExpressionRangeFilterStrategy(
            "$EXPENSE_ALIAS.amount",
            ExpenseSearchParameterNames.MIN_AMOUNT,
            expenseSearchParameters.minAmount,
            ExpenseSearchParameterNames.MAX_AMOUNT,
            expenseSearchParameters.maxAmount
        ).apply(context)

        // EXISTS rather than a join: an expense paid with several of the requested methods must stay one row.
        if (expenseSearchParameters.paymentMethodIds.isNotEmpty()) {
            context.whereClauses.add(
                """
                EXISTS (
                  SELECT 1 FROM ${tables.expensePayment} $EXPENSE_PAYMENT_ALIAS
                  WHERE $EXPENSE_PAYMENT_ALIAS.expense_id = $EXPENSE_ALIAS.id
                    AND $EXPENSE_PAYMENT_ALIAS.payment_method_id = ANY(:${ExpenseSearchParameterNames.PAYMENT_METHOD_IDS})
                    AND NOT EXISTS (
                      SELECT 1 FROM ${tables.expensePaymentVoid} $EXPENSE_PAYMENT_VOID_ALIAS
                      WHERE $EXPENSE_PAYMENT_VOID_ALIAS.expense_payment_id = $EXPENSE_PAYMENT_ALIAS.id
                    )
                )
                """.trimIndent()
            )
            context.params[ExpenseSearchParameterNames.PAYMENT_METHOD_IDS] = expenseSearchParameters.paymentMethodIds.toTypedArray()
        }

        expenseSearchParameters.voided?.let { voided ->
            context.whereClauses.add(if (voided) "$EXPENSE_VOID_ALIAS.id IS NOT NULL" else "$EXPENSE_VOID_ALIAS.id IS NULL")
        }

        return context
    }

    fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?, tables: ExpenseSearchTables): SqlQuery {
        val whereClauses = predicate.whereClauses.toMutableList()
        val params = predicate.params.toMutableMap()

        if (cursor != null) {
            whereClauses.add(
                "($EXPENSE_ALIAS.created_on, $EXPENSE_ALIAS.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
            )
            params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
            params[QueryParameterNames.CURSOR_ID] = cursor.id
        }

        val sql = """
            SELECT
              $EXPENSE_ALIAS.id AS id,
              $EXPENSE_ALIAS.reference_number AS reference_number,
              $EXPENSE_ALIAS.created_on AS created_on
            ${fromAndJoins(tables)}
            WHERE ${whereClauses.joinToString(" AND ")}
            ORDER BY $EXPENSE_ALIAS.created_on DESC, $EXPENSE_ALIAS.id DESC
            LIMIT :${QueryParameterNames.PAGE_SIZE}
        """.trimIndent()

        return SqlQuery(sql, params, ExpenseSearchQueryMetadata("expense_search"))
    }
    fun buildSummaryQuery(predicate: QueryBuilderContext, tables: ExpenseSearchTables): SqlQuery {
        val sql = """
            SELECT
              $EXPENSE_ALIAS.expense_type_id AS expense_type_id,
              $IS_VOIDED AS voided,
              $PAYMENT_STATE_ALIAS.payment_status AS payment_status,
              COUNT(*) AS expense_count,
              COALESCE(SUM($EXPENSE_ALIAS.amount), 0) AS amount_total,
              COALESCE(SUM(CASE WHEN $EXPENSE_VOID_ALIAS.id IS NULL THEN $PAYMENT_STATE_ALIAS.amount_paid ELSE 0 END), 0) AS paid_total,
              COALESCE(SUM(CASE WHEN $EXPENSE_VOID_ALIAS.id IS NULL THEN GREATEST($EXPENSE_ALIAS.amount - $PAYMENT_STATE_ALIAS.amount_paid, 0) ELSE 0 END), 0) AS outstanding_total
            ${fromAndJoins(tables)}
            WHERE ${predicate.whereClauses.joinToString(" AND ")}
            GROUP BY $EXPENSE_ALIAS.expense_type_id, $IS_VOIDED, $PAYMENT_STATE_ALIAS.payment_status
        """.trimIndent()

        return SqlQuery(sql, predicate.params, ExpenseSearchQueryMetadata("expense_search_summary"))
    }
}
