package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentSearchParameters
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters.AmountRangeFilterStrategy
import me.ezra_home.retail_software_solution.locations.business.sale_payment.search.filters.SalePaymentStatusFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object SalePaymentSearchQueryBuilder {

  private const val FROM_AND_JOINS = """
    FROM ${TableNames.SALE_PAYMENT} ${SalePaymentSearchAliases.SALE_PAYMENT}
    INNER JOIN ${TableNames.SALE} ${SalePaymentSearchAliases.SALE}
      ON ${SalePaymentSearchAliases.SALE}.id = ${SalePaymentSearchAliases.SALE_PAYMENT}.sale_id
    LEFT JOIN ${TableNames.SALE_PAYMENT_VOID} ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}
      ON ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.sale_payment_id = ${SalePaymentSearchAliases.SALE_PAYMENT}.id
  """

  fun buildPredicate(salePaymentSearchParameters: SalePaymentSearchParameters): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("1=1")

    DateRangeFilterStrategy(
      "${SalePaymentSearchAliases.SALE_PAYMENT}.created_on",
      SalePaymentSearchParameterNames.RECORDED_FROM,
      salePaymentSearchParameters.recordedFrom,
      SalePaymentSearchParameterNames.RECORDED_BEFORE,
      salePaymentSearchParameters.recordedBefore
    ).apply(context)

    DateRangeFilterStrategy(
      "${SalePaymentSearchAliases.SALE_PAYMENT}.payment_date",
      SalePaymentSearchParameterNames.PAYMENT_DATE_FROM,
      salePaymentSearchParameters.paymentDateFrom,
      SalePaymentSearchParameterNames.PAYMENT_DATE_BEFORE,
      salePaymentSearchParameters.paymentDateBefore
    ).apply(context)

    AnyListFilterStrategy(
      "${SalePaymentSearchAliases.SALE}.contact_id",
      SalePaymentSearchParameterNames.CONTACT_IDS,
      salePaymentSearchParameters.contactIds.toTypedArray()
    ).apply(context)
    AnyListFilterStrategy(
      "${SalePaymentSearchAliases.SALE_PAYMENT}.payment_method_id",
      SalePaymentSearchParameterNames.PAYMENT_METHOD_IDS,
      salePaymentSearchParameters.paymentMethodIds.toTypedArray()
    ).apply(context)
    SalePaymentStatusFilterStrategy(salePaymentSearchParameters.statuses).apply(context)
    AmountRangeFilterStrategy(salePaymentSearchParameters.minAmount, salePaymentSearchParameters.maxAmount).apply(context)
    AnyListFilterStrategy(
      "${SalePaymentSearchAliases.SALE}.reference_number",
      SalePaymentSearchParameterNames.SALE_REFERENCE_NUMBERS,
      salePaymentSearchParameters.saleReferenceNumbers.toTypedArray()
    ).apply(context)
    return context
  }

  fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
    val whereClauses = predicate.whereClauses.toMutableList()
    val params = predicate.params.toMutableMap()

    if (cursor != null) {
      whereClauses.add(
        "(${SalePaymentSearchAliases.SALE_PAYMENT}.created_on, ${SalePaymentSearchAliases.SALE_PAYMENT}.id) < " +
          "(:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
      )
      params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
      params[QueryParameterNames.CURSOR_ID] = cursor.id
    }

    val sql = """
      SELECT
        ${SalePaymentSearchAliases.SALE_PAYMENT}.id AS id,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.reference_number AS reference_number,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.created_on AS created_on,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.payment_date AS payment_date,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.sale_id AS sale_id,
        ${SalePaymentSearchAliases.SALE}.reference_number AS sale_reference_number,
        ${SalePaymentSearchAliases.SALE}.contact_id AS contact_id,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.payment_method_id AS payment_method_id,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.amount AS amount,
        ${SalePaymentSearchAliases.SALE_PAYMENT}.reference AS reference,
        ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.reason AS void_reason
      $FROM_AND_JOINS
      WHERE ${whereClauses.joinToString(" AND ")}
      ORDER BY ${SalePaymentSearchAliases.SALE_PAYMENT}.created_on DESC, ${SalePaymentSearchAliases.SALE_PAYMENT}.id DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()

    return SqlQuery(sql, params, SalePaymentSearchQueryMetadata("sale_payment_search"))
  }

  fun buildSummaryQuery(predicate: QueryBuilderContext): SqlQuery {
    val sql = """
      SELECT
        ${SalePaymentSearchAliases.SALE_PAYMENT}.payment_method_id AS payment_method_id,
        COALESCE(SUM(${SalePaymentSearchAliases.SALE_PAYMENT}.amount) FILTER (WHERE ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.id IS NULL), 0) AS active_total,
        COALESCE(SUM(${SalePaymentSearchAliases.SALE_PAYMENT}.amount) FILTER (WHERE ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.id IS NOT NULL), 0) AS voided_total,
        COUNT(*) FILTER (WHERE ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.id IS NULL) AS active_count,
        COUNT(*) FILTER (WHERE ${SalePaymentSearchAliases.SALE_PAYMENT_VOID}.id IS NOT NULL) AS voided_count
      $FROM_AND_JOINS
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY ${SalePaymentSearchAliases.SALE_PAYMENT}.payment_method_id
    """.trimIndent()

    return SqlQuery(sql, predicate.params, SalePaymentSearchQueryMetadata("sale_payment_search_summary"))
  }
}
