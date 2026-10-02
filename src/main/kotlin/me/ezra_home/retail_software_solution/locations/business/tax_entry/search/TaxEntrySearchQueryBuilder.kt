package me.ezra_home.retail_software_solution.locations.business.tax_entry.search

import me.ezra_home.retail_software_solution.locations.business.tax_entry.api.TaxEntrySearchParameters
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.ExpressionRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object TaxEntrySearchQueryBuilder {

    private const val FROM = "FROM ${TableNames.TAX_ENTRY} ${TaxEntrySearchAliases.TAX_ENTRY}"

    fun buildPredicate(taxEntrySearchParameters: TaxEntrySearchParameters): QueryBuilderContext {
        val context = QueryBuilderContext()
        context.whereClauses.add("1=1")

        AnyListFilterStrategy(
            "${TaxEntrySearchAliases.TAX_ENTRY}.fiscal_period_id",
            TaxEntrySearchParameterNames.FISCAL_PERIOD_IDS,
            taxEntrySearchParameters.fiscalPeriodIds.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "${TaxEntrySearchAliases.TAX_ENTRY}.tax_type_id",
            TaxEntrySearchParameterNames.TAX_TYPE_IDS,
            taxEntrySearchParameters.taxTypeIds.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "${TaxEntrySearchAliases.TAX_ENTRY}.source_type",
            TaxEntrySearchParameterNames.SOURCE_TYPES,
            taxEntrySearchParameters.sourceTypes.map { it.code }.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "${TaxEntrySearchAliases.TAX_ENTRY}.source_reference_number",
            TaxEntrySearchParameterNames.SOURCE_REFERENCE_NUMBERS,
            taxEntrySearchParameters.sourceReferenceNumbers.toTypedArray()
        ).apply(context)

        // min/max bound the magnitude of tax_amount, not its signed value, so a range matches an
        // original and its reversal symmetrically (a $15 original and its -$15 reversal both match
        // min=10/max=20). Sums in buildSummaryQuery stay signed — only this filter is magnitude-based.
        ExpressionRangeFilterStrategy(
            "ABS(${TaxEntrySearchAliases.TAX_ENTRY}.tax_amount)",
            TaxEntrySearchParameterNames.MIN_TAX_AMOUNT,
            taxEntrySearchParameters.minTaxAmount,
            TaxEntrySearchParameterNames.MAX_TAX_AMOUNT,
            taxEntrySearchParameters.maxTaxAmount
        ).apply(context)

        return context
    }

    fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
        val whereClauses = predicate.whereClauses.toMutableList()
        val params = predicate.params.toMutableMap()

        if (cursor != null) {
            whereClauses.add(
                "(${TaxEntrySearchAliases.TAX_ENTRY}.created_on, ${TaxEntrySearchAliases.TAX_ENTRY}.id) < " +
                    "(:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
            )
            params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
            params[QueryParameterNames.CURSOR_ID] = cursor.id
        }

        val sql = """
            SELECT
                ${TaxEntrySearchAliases.TAX_ENTRY}.id AS id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.source_reference_number AS source_reference_number,
                ${TaxEntrySearchAliases.TAX_ENTRY}.source_type AS source_type,
                ${TaxEntrySearchAliases.TAX_ENTRY}.direction AS direction,
                ${TaxEntrySearchAliases.TAX_ENTRY}.tax_type_id AS tax_type_id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.fiscal_period_id AS fiscal_period_id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.calculation_method AS calculation_method,
                ${TaxEntrySearchAliases.TAX_ENTRY}.rate AS rate,
                ${TaxEntrySearchAliases.TAX_ENTRY}.tax_inclusive AS tax_inclusive,
                ${TaxEntrySearchAliases.TAX_ENTRY}.taxable_amount AS taxable_amount,
                ${TaxEntrySearchAliases.TAX_ENTRY}.tax_amount AS tax_amount,
                ${TaxEntrySearchAliases.TAX_ENTRY}.created_on AS created_on
            $FROM
            WHERE ${whereClauses.joinToString(" AND ")}
            ORDER BY ${TaxEntrySearchAliases.TAX_ENTRY}.created_on DESC, ${TaxEntrySearchAliases.TAX_ENTRY}.id DESC
            LIMIT :${QueryParameterNames.PAGE_SIZE}
        """.trimIndent()

        return SqlQuery(sql, params, TaxEntrySearchQueryMetadata("tax_entry_search"))
    }

    fun buildSummaryQuery(predicate: QueryBuilderContext): SqlQuery {
        val sql = """
            SELECT
                ${TaxEntrySearchAliases.TAX_ENTRY}.fiscal_period_id AS fiscal_period_id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.tax_type_id AS tax_type_id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.source_type AS source_type,
                COUNT(*) AS entry_count,
                COALESCE(SUM(${TaxEntrySearchAliases.TAX_ENTRY}.taxable_amount), 0) AS taxable_amount,
                COALESCE(SUM(${TaxEntrySearchAliases.TAX_ENTRY}.tax_amount), 0) AS tax_amount
            $FROM
            WHERE ${predicate.whereClauses.joinToString(" AND ")}
            GROUP BY
                ${TaxEntrySearchAliases.TAX_ENTRY}.fiscal_period_id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.tax_type_id,
                ${TaxEntrySearchAliases.TAX_ENTRY}.source_type
        """.trimIndent()

        return SqlQuery(sql, predicate.params, TaxEntrySearchQueryMetadata("tax_entry_search_summary"))
    }
}
