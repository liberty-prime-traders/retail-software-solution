package me.ezra_home.retail_software_solution.locations.business.sale.search

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSearchParameters
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.ExpressionRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object SaleSearchQueryBuilder {

  private const val S = SaleSearchAliases.SALE

  private const val SUMMARY_FROM_AND_JOINS = """
    FROM ${TableNames.SALE} $S
    LEFT JOIN LATERAL (
      SELECT COALESCE(SUM(${SaleSearchAliases.SALE_PAYMENT}.amount) FILTER (WHERE ${SaleSearchAliases.SALE_PAYMENT_VOID}.id IS NULL), 0) AS paid
      FROM ${TableNames.SALE_PAYMENT} ${SaleSearchAliases.SALE_PAYMENT}
      LEFT JOIN ${TableNames.SALE_PAYMENT_VOID} ${SaleSearchAliases.SALE_PAYMENT_VOID}
        ON ${SaleSearchAliases.SALE_PAYMENT_VOID}.sale_payment_id = ${SaleSearchAliases.SALE_PAYMENT}.id
      WHERE ${SaleSearchAliases.SALE_PAYMENT}.sale_id = $S.id
    ) ${SaleSearchAliases.PAYMENTS} ON TRUE
  """

  fun buildPredicate(saleSearchParameters: SaleSearchParameters): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("1=1")

    DateRangeFilterStrategy(
      "$S.created_on",
      SaleSearchParameterNames.CREATED_FROM,
      saleSearchParameters.createdFrom,
      SaleSearchParameterNames.CREATED_BEFORE,
      saleSearchParameters.createdBefore
    ).apply(context)

    AnyListFilterStrategy(
      "$S.contact_id",
      SaleSearchParameterNames.CONTACT_IDS,
      saleSearchParameters.contactIds.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$S.sold_by_id",
      SaleSearchParameterNames.SOLD_BY_USER_IDS,
      saleSearchParameters.soldByUserIds.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$S.reference_number",
      SaleSearchParameterNames.SALE_REFERENCE_NUMBERS,
      saleSearchParameters.saleReferenceNumbers.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$S.status",
      SaleSearchParameterNames.SALE_STATUSES,
      saleSearchParameters.saleStatuses.map { it.code }.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$S.payment_status",
      SaleSearchParameterNames.PAYMENT_STATUSES,
      saleSearchParameters.paymentStatuses.map { it.code }.toTypedArray()
    ).apply(context)

    ExpressionRangeFilterStrategy(
      SaleSearchExpressions.RECEIVABLE_TOTAL,
      SaleSearchParameterNames.MIN_RECEIVABLE_TOTAL,
      saleSearchParameters.minReceivableTotal,
      SaleSearchParameterNames.MAX_RECEIVABLE_TOTAL,
      saleSearchParameters.maxReceivableTotal
    ).apply(context)

    ExpressionRangeFilterStrategy(
      SaleSearchExpressions.DISCOUNT_TOTAL,
      SaleSearchParameterNames.MIN_DISCOUNT_TOTAL,
      saleSearchParameters.minDiscountTotal,
      SaleSearchParameterNames.MAX_DISCOUNT_TOTAL,
      saleSearchParameters.maxDiscountTotal
    ).apply(context)

    return context
  }

  fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
    val whereClauses = predicate.whereClauses.toMutableList()
    val params = predicate.params.toMutableMap()

    if (cursor != null) {
      whereClauses.add(
        "($S.created_on, $S.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
      )
      params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
      params[QueryParameterNames.CURSOR_ID] = cursor.id
    }

    val sql = """
      SELECT
        $S.id AS id,
        $S.reference_number AS reference_number,
        $S.contact_id AS contact_id,
        $S.sold_by_id AS sold_by_id,
        $S.date_sold AS date_sold,
        $S.created_on AS created_on,
        $S.status AS status,
        $S.payment_status AS payment_status,
        ${SaleSearchExpressions.RECEIVABLE_TOTAL} AS receivable_total
      FROM ${TableNames.SALE} $S
      WHERE ${whereClauses.joinToString(" AND ")}
      ORDER BY $S.created_on DESC, $S.id DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()

    return SqlQuery(sql, params, SaleSearchQueryMetadata("sale_search"))
  }

  // ELSE paid on credit_total is correct for drafts (deposits) and harmless for voided/discarded,
  // where paid is zero by the void/discard-ordering invariant: voiding or discarding a sale is
  // blocked until its payments are voided. If a path ever breaks that ordering, credit_total goes
  // silently wrong here rather than failing.
  fun buildSummaryQuery(predicate: QueryBuilderContext): SqlQuery {
    val receivableTotal = SaleSearchExpressions.RECEIVABLE_TOTAL
    val discountTotal = SaleSearchExpressions.DISCOUNT_TOTAL
    val paid = "${SaleSearchAliases.PAYMENTS}.paid"
    val confirmedStatus = ":${SaleSearchParameterNames.CONFIRMED_STATUS}"

    val params = predicate.params.toMutableMap()
    params[SaleSearchParameterNames.CONFIRMED_STATUS] = SaleStatus.CONFIRMED.code

    val sql = """
      SELECT
        $S.status AS status,
        COUNT(*) AS sale_count,
        COALESCE(SUM($receivableTotal), 0) AS receivable_total,
        COALESCE(SUM($discountTotal), 0) AS discount_total,
        COALESCE(SUM($paid), 0) AS paid_total,
        COALESCE(SUM(CASE WHEN $S.status = $confirmedStatus
                          THEN GREATEST($receivableTotal - $paid, 0)
                          ELSE 0 END), 0) AS outstanding_total,
        COALESCE(SUM(CASE WHEN $S.status = $confirmedStatus
                          THEN GREATEST($paid - $receivableTotal, 0)
                          ELSE $paid END), 0) AS credit_total
      $SUMMARY_FROM_AND_JOINS
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY $S.status
    """.trimIndent()

    return SqlQuery(sql, params, SaleSearchQueryMetadata("sale_search_summary"))
  }
}
