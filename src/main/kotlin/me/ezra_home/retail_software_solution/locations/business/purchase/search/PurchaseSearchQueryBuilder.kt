package me.ezra_home.retail_software_solution.locations.business.purchase.search

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseSearchParameters
import me.ezra_home.retail_software_solution.locations.business.purchase.search.filters.AmountRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery

object PurchaseSearchQueryBuilder {

  // LATERAL and correlated on purchase_id (indexed via idx_purchase_line_purchase_id) so the planner
  // filters `purchase` first and only sums each surviving row's own lines — not the whole table.
  private const val ORDERED_TOTAL_JOIN = """
    LEFT JOIN LATERAL (
      SELECT COALESCE(SUM(ROUND(unit_cost * (quantity_ordered - quantity_canceled), 4)), 0) AS ordered_total
      FROM ${TableNames.PURCHASE_LINE}
      WHERE purchase_id = ${PurchaseSearchAliases.PURCHASE}.id
    ) ${PurchaseSearchAliases.ORDERED_TOTAL} ON true
  """

  // Same LATERAL correlation, via idx_supplier_payment_purchase_id. Excludes voided payments so every
  // paid/outstanding figure downstream already reflects that.
  private const val PAID_TOTAL_JOIN = """
    LEFT JOIN LATERAL (
      SELECT COALESCE(SUM(sp.amount), 0) AS paid_total
      FROM ${TableNames.SUPPLIER_PAYMENT} sp
      WHERE sp.purchase_id = ${PurchaseSearchAliases.PURCHASE}.id
        AND NOT EXISTS (
          SELECT 1 FROM ${TableNames.SUPPLIER_PAYMENT_VOID} spv WHERE spv.supplier_payment_id = sp.id
        )
    ) ${PurchaseSearchAliases.PAID_TOTAL} ON true
  """

  private const val LIST_FROM_ONLY = "FROM ${TableNames.PURCHASE} ${PurchaseSearchAliases.PURCHASE}"

  private const val LIST_FROM_WITH_ORDERED_TOTAL = "$LIST_FROM_ONLY $ORDERED_TOTAL_JOIN"

  private const val SUMMARY_FROM_AND_JOINS =
    "FROM ${TableNames.PURCHASE} ${PurchaseSearchAliases.PURCHASE} $ORDERED_TOTAL_JOIN $PAID_TOTAL_JOIN"

  fun buildPredicate(purchaseSearchParameters: PurchaseSearchParameters): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("1=1")

    DateRangeFilterStrategy(
      "${PurchaseSearchAliases.PURCHASE}.created_on",
      PurchaseSearchParameterNames.RECORDED_FROM,
      purchaseSearchParameters.recordedFrom,
      PurchaseSearchParameterNames.RECORDED_BEFORE,
      purchaseSearchParameters.recordedBefore
    ).apply(context)

    DateRangeFilterStrategy(
      "${PurchaseSearchAliases.PURCHASE}.date_ordered",
      PurchaseSearchParameterNames.PURCHASE_DATE_FROM,
      purchaseSearchParameters.purchaseDateFrom,
      PurchaseSearchParameterNames.PURCHASE_DATE_BEFORE,
      purchaseSearchParameters.purchaseDateBefore
    ).apply(context)

    AnyListFilterStrategy(
      "${PurchaseSearchAliases.PURCHASE}.supplier_id",
      PurchaseSearchParameterNames.SUPPLIER_IDS,
      purchaseSearchParameters.supplierIds.toTypedArray()
    ).apply(context)
    AnyListFilterStrategy(
      "${PurchaseSearchAliases.PURCHASE}.purchase_status",
      PurchaseSearchParameterNames.PURCHASE_STATUSES,
      purchaseSearchParameters.purchaseStatuses.map { it.code }.toTypedArray()
    ).apply(context)
    AnyListFilterStrategy(
      "${PurchaseSearchAliases.PURCHASE}.payment_status",
      PurchaseSearchParameterNames.PAYMENT_STATUSES,
      purchaseSearchParameters.paymentStatuses.map { it.code }.toTypedArray()
    ).apply(context)
    AmountRangeFilterStrategy(purchaseSearchParameters.minAmount, purchaseSearchParameters.maxAmount).apply(context)
    AnyListFilterStrategy(
      "${PurchaseSearchAliases.PURCHASE}.reference_number",
      PurchaseSearchParameterNames.PURCHASE_REFERENCE_NUMBERS,
      purchaseSearchParameters.purchaseReferenceNumbers.toTypedArray()
    ).apply(context)
    return context
  }

  fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
    val whereClauses = predicate.whereClauses.toMutableList()
    val params = predicate.params.toMutableMap()

    if (cursor != null) {
      whereClauses.add(
        "(${PurchaseSearchAliases.PURCHASE}.created_on, ${PurchaseSearchAliases.PURCHASE}.id) < " +
          "(:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
      )
      params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
      params[QueryParameterNames.CURSOR_ID] = cursor.id
    }

    val needsOrderedTotal = PurchaseSearchParameterNames.MIN_AMOUNT in params || PurchaseSearchParameterNames.MAX_AMOUNT in params
    val fromAndJoins = if (needsOrderedTotal) LIST_FROM_WITH_ORDERED_TOTAL else LIST_FROM_ONLY

    val sql = """
      SELECT ${PurchaseSearchAliases.PURCHASE}.*
      $fromAndJoins
      WHERE ${whereClauses.joinToString(" AND ")}
      ORDER BY ${PurchaseSearchAliases.PURCHASE}.created_on DESC, ${PurchaseSearchAliases.PURCHASE}.id DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()

    return SqlQuery(sql, params, PurchaseSearchQueryMetadata("purchase_search"))
  }

  fun buildPaymentStatusSummaryQuery(predicate: QueryBuilderContext): SqlQuery {
    val sql = """
      SELECT
        ${PurchaseSearchAliases.PURCHASE}.payment_status AS payment_status,
        COUNT(*) AS purchase_count,
        COALESCE(SUM(${PurchaseSearchAliases.ORDERED_TOTAL}.ordered_total), 0) AS total_ordered,
        COALESCE(SUM(${PurchaseSearchAliases.PAID_TOTAL}.paid_total), 0) AS total_paid
      $SUMMARY_FROM_AND_JOINS
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY ${PurchaseSearchAliases.PURCHASE}.payment_status
    """.trimIndent()

    return SqlQuery(sql, predicate.params, PurchaseSearchQueryMetadata("purchase_search_summary_by_payment_status"))
  }

  fun buildSupplierSummaryQuery(predicate: QueryBuilderContext): SqlQuery {
    val sql = """
      SELECT
        ${PurchaseSearchAliases.PURCHASE}.supplier_id AS supplier_id,
        COUNT(*) AS purchase_count,
        COALESCE(SUM(${PurchaseSearchAliases.ORDERED_TOTAL}.ordered_total), 0) AS total_ordered,
        COALESCE(SUM(${PurchaseSearchAliases.PAID_TOTAL}.paid_total), 0) AS total_paid
      $SUMMARY_FROM_AND_JOINS
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY ${PurchaseSearchAliases.PURCHASE}.supplier_id
    """.trimIndent()

    return SqlQuery(sql, predicate.params, PurchaseSearchQueryMetadata("purchase_search_summary_by_supplier"))
  }
}
