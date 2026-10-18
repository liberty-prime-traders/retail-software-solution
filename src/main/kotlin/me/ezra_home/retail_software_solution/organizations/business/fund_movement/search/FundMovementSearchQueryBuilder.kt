package me.ezra_home.retail_software_solution.organizations.business.fund_movement.search

import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementSearchParameters
import me.ezra_home.retail_software_solution.organizations.business.fund_movement.api.FundMovementStatus
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object FundMovementSearchQueryBuilder {

    private const val FUND_MOVEMENT_ALIAS = "fm"
    private const val FUND_MOVEMENT_VOID_ALIAS = "fmv"

    private const val FROM_AND_JOINS = """
        FROM ${TableNames.FUND_MOVEMENT} $FUND_MOVEMENT_ALIAS
        LEFT JOIN ${TableNames.FUND_MOVEMENT_VOID} $FUND_MOVEMENT_VOID_ALIAS ON $FUND_MOVEMENT_VOID_ALIAS.fund_movement_id = $FUND_MOVEMENT_ALIAS.id
    """

    fun buildPredicate(fundMovementSearchParameters: FundMovementSearchParameters): QueryBuilderContext {
        val context = QueryBuilderContext()
        context.whereClauses.add("1=1")

        DateRangeFilterStrategy(
            "$FUND_MOVEMENT_ALIAS.created_on",
            FundMovementSearchParameterNames.CREATED_FROM,
            fundMovementSearchParameters.createdFrom,
            FundMovementSearchParameterNames.CREATED_BEFORE,
            fundMovementSearchParameters.createdBefore
        ).apply(context)

        DateRangeFilterStrategy(
            "$FUND_MOVEMENT_ALIAS.movement_date",
            FundMovementSearchParameterNames.MOVEMENT_DATE_FROM,
            fundMovementSearchParameters.movementDateFrom,
            FundMovementSearchParameterNames.MOVEMENT_DATE_BEFORE,
            fundMovementSearchParameters.movementDateBefore
        ).apply(context)

        AnyListFilterStrategy(
            "$FUND_MOVEMENT_ALIAS.type",
            FundMovementSearchParameterNames.TYPES,
            fundMovementSearchParameters.types.map { it.code }.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "$FUND_MOVEMENT_ALIAS.reference_number",
            FundMovementSearchParameterNames.REFERENCES,
            fundMovementSearchParameters.references.toTypedArray()
        ).apply(context)

        // A single-row predicate over both sides: the movement stays one row however many requested accounts it touches.
        if (fundMovementSearchParameters.accountCodes.isNotEmpty()) {
            context.whereClauses.add(
                "($FUND_MOVEMENT_ALIAS.debit_account_code = ANY(:${FundMovementSearchParameterNames.ACCOUNT_CODES})" +
                    " OR $FUND_MOVEMENT_ALIAS.credit_account_code = ANY(:${FundMovementSearchParameterNames.ACCOUNT_CODES}))"
            )
            context.params[FundMovementSearchParameterNames.ACCOUNT_CODES] = fundMovementSearchParameters.accountCodes.toTypedArray()
        }

        if (fundMovementSearchParameters.statuses.size == 1) {
            context.whereClauses.add(
                when (fundMovementSearchParameters.statuses.single()) {
                    FundMovementStatus.VOIDED -> "$FUND_MOVEMENT_VOID_ALIAS.id IS NOT NULL"
                    FundMovementStatus.ACTIVE -> "$FUND_MOVEMENT_VOID_ALIAS.id IS NULL"
                }
            )
        }

        return context
    }

    fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
        val whereClauses = predicate.whereClauses.toMutableList()
        val params = predicate.params.toMutableMap()

        if (cursor != null) {
            whereClauses.add(
                "($FUND_MOVEMENT_ALIAS.created_on, $FUND_MOVEMENT_ALIAS.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
            )
            params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
            params[QueryParameterNames.CURSOR_ID] = cursor.id
        }

        val sql = """
            SELECT
              $FUND_MOVEMENT_ALIAS.id AS id,
              $FUND_MOVEMENT_ALIAS.reference_number AS reference_number,
              $FUND_MOVEMENT_ALIAS.type AS type,
              $FUND_MOVEMENT_ALIAS.debit_account_code AS debit_account_code,
              $FUND_MOVEMENT_ALIAS.credit_account_code AS credit_account_code,
              $FUND_MOVEMENT_ALIAS.amount AS amount,
              $FUND_MOVEMENT_ALIAS.movement_date AS movement_date,
              $FUND_MOVEMENT_ALIAS.note AS note,
              $FUND_MOVEMENT_ALIAS.created_by_id AS created_by_id,
              $FUND_MOVEMENT_ALIAS.created_on AS created_on,
              $FUND_MOVEMENT_VOID_ALIAS.id AS void_id,
              $FUND_MOVEMENT_VOID_ALIAS.reference_number AS void_reference_number,
              $FUND_MOVEMENT_VOID_ALIAS.void_date AS void_date,
              $FUND_MOVEMENT_VOID_ALIAS.reason AS void_reason,
              $FUND_MOVEMENT_VOID_ALIAS.created_by_id AS void_created_by_id,
              $FUND_MOVEMENT_VOID_ALIAS.created_on AS void_created_on
            $FROM_AND_JOINS
            WHERE ${whereClauses.joinToString(" AND ")}
            ORDER BY $FUND_MOVEMENT_ALIAS.created_on DESC, $FUND_MOVEMENT_ALIAS.id DESC
            LIMIT :${QueryParameterNames.PAGE_SIZE}
        """.trimIndent()

        return SqlQuery(sql, params, FundMovementSearchQueryMetadata("fund_movement_search"))
    }
}
