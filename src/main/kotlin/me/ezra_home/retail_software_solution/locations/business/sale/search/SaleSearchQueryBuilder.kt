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

  private const val SALE_ALIAS = SaleSearchAliases.SALE

  private const val SUMMARY_FROM_AND_JOINS = """
    FROM ${TableNames.SALE} $SALE_ALIAS
    LEFT JOIN LATERAL (
      SELECT COALESCE(SUM(${SaleSearchAliases.SALE_PAYMENT}.amount) FILTER (WHERE ${SaleSearchAliases.SALE_PAYMENT_VOID}.id IS NULL), 0) AS paid
      FROM ${TableNames.SALE_PAYMENT} ${SaleSearchAliases.SALE_PAYMENT}
      LEFT JOIN ${TableNames.SALE_PAYMENT_VOID} ${SaleSearchAliases.SALE_PAYMENT_VOID}
        ON ${SaleSearchAliases.SALE_PAYMENT_VOID}.sale_payment_id = ${SaleSearchAliases.SALE_PAYMENT}.id
      WHERE ${SaleSearchAliases.SALE_PAYMENT}.sale_id = $SALE_ALIAS.id
    ) ${SaleSearchAliases.PAYMENTS} ON TRUE
  """

  fun buildPredicate(saleSearchParameters: SaleSearchParameters): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("1=1")

    DateRangeFilterStrategy(
      "$SALE_ALIAS.created_on",
      SaleSearchParameterNames.CREATED_FROM,
      saleSearchParameters.createdFrom,
      SaleSearchParameterNames.CREATED_BEFORE,
      saleSearchParameters.createdBefore
    ).apply(context)

    AnyListFilterStrategy(
      "$SALE_ALIAS.contact_id",
      SaleSearchParameterNames.CONTACT_IDS,
      saleSearchParameters.contactIds.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$SALE_ALIAS.sold_by_id",
      SaleSearchParameterNames.SOLD_BY_USER_IDS,
      saleSearchParameters.soldByUserIds.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$SALE_ALIAS.reference_number",
      SaleSearchParameterNames.SALE_REFERENCE_NUMBERS,
      saleSearchParameters.saleReferenceNumbers.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$SALE_ALIAS.status",
      SaleSearchParameterNames.SALE_STATUSES,
      saleSearchParameters.saleStatuses.map { it.code }.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$SALE_ALIAS.payment_status",
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
        "($SALE_ALIAS.created_on, $SALE_ALIAS.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
      )
      params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
      params[QueryParameterNames.CURSOR_ID] = cursor.id
    }

    val sql = """
      SELECT
        $SALE_ALIAS.id AS id,
        $SALE_ALIAS.reference_number AS reference_number,
        $SALE_ALIAS.contact_id AS contact_id,
        $SALE_ALIAS.sold_by_id AS sold_by_id,
        $SALE_ALIAS.date_sold AS date_sold,
        $SALE_ALIAS.created_on AS created_on,
        $SALE_ALIAS.status AS status,
        $SALE_ALIAS.payment_status AS payment_status,
        ${SaleSearchExpressions.RECEIVABLE_TOTAL} AS receivable_total
      FROM ${TableNames.SALE} $SALE_ALIAS
      WHERE ${whereClauses.joinToString(" AND ")}
      ORDER BY $SALE_ALIAS.created_on DESC, $SALE_ALIAS.id DESC
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
        $SALE_ALIAS.status AS status,
        COUNT(*) AS sale_count,
        COALESCE(SUM($receivableTotal), 0) AS receivable_total,
        COALESCE(SUM($discountTotal), 0) AS discount_total,
        COALESCE(SUM($paid), 0) AS paid_total,
        COALESCE(SUM(CASE WHEN $SALE_ALIAS.status = $confirmedStatus
                          THEN GREATEST($receivableTotal - $paid, 0)
                          ELSE 0 END), 0) AS outstanding_total,
        COALESCE(SUM(CASE WHEN $SALE_ALIAS.status = $confirmedStatus
                          THEN GREATEST($paid - $receivableTotal, 0)
                          ELSE $paid END), 0) AS credit_total
      $SUMMARY_FROM_AND_JOINS
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY $SALE_ALIAS.status
    """.trimIndent()

    return SqlQuery(sql, params, SaleSearchQueryMetadata("sale_search_summary"))
  }
}
