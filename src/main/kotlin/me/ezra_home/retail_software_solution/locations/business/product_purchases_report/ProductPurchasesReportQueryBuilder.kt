package me.ezra_home.retail_software_solution.locations.business.product_purchases_report

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesDeliveryLinesParameters
import me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api.ProductPurchasesReportParameters
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import java.util.UUID

object ProductPurchasesReportQueryBuilder {

  private const val DELIVERY_LINE_ALIAS = "pdl"
  private const val DELIVERY_ALIAS = "pd"
  private const val PURCHASE_LINE_ALIAS = "pl"
  private const val PURCHASE_ALIAS = "p"
  private const val LOCATION_PRODUCT_ALIAS = "lp"

  private const val DELIVERED_FROM = "deliveredFrom"
  private const val DELIVERED_BEFORE = "deliveredBefore"
  private const val LOCATION_PRODUCT_IDS = "locationProductIds"
  private const val LOCATION_PRODUCT_ID = "locationProductId"
  private const val CATEGORY_IDS = "categoryIds"
  private const val SUPPLIER_IDS = "supplierIds"
  private const val APPLIED_DELIVERY_STATUSES = "appliedDeliveryStatuses"
  private const val ACTIVE_LINE_STATUS = "activeLineStatus"
  private const val MONEY_SCALE = "moneyScale"

  private val FROM_AND_JOINS = """
    FROM ${TableNames.PURCHASE_DELIVERY_LINE} $DELIVERY_LINE_ALIAS
    JOIN ${TableNames.PURCHASE_DELIVERY} $DELIVERY_ALIAS ON $DELIVERY_ALIAS.id = $DELIVERY_LINE_ALIAS.purchase_delivery_id
    JOIN ${TableNames.PURCHASE_LINE} $PURCHASE_LINE_ALIAS ON $PURCHASE_LINE_ALIAS.id = $DELIVERY_LINE_ALIAS.purchase_line_id
    JOIN ${TableNames.PURCHASE} $PURCHASE_ALIAS ON $PURCHASE_ALIAS.id = $PURCHASE_LINE_ALIAS.purchase_id
    JOIN ${TableNames.LOCATION_PRODUCT} $LOCATION_PRODUCT_ALIAS ON $LOCATION_PRODUCT_ALIAS.id = $PURCHASE_LINE_ALIAS.location_product_id
  """.trimIndent()

  // Revised lines stay in the expansion and are excluded from the summary totals.
  fun buildPredicate(
    productPurchasesReportParameters: ProductPurchasesReportParameters,
    deliveredRange: ProductPurchasesDeliveredRange
  ): QueryBuilderContext {
    val context = buildReceivedDeliveryPredicate(deliveredRange, productPurchasesReportParameters.supplierIds)
    context.whereClauses.add("$DELIVERY_LINE_ALIAS.status = :$ACTIVE_LINE_STATUS")
    context.params[ACTIVE_LINE_STATUS] = PurchaseDeliveryLineStatus.ACTIVE.code

    AnyListFilterStrategy(
      "$PURCHASE_LINE_ALIAS.location_product_id",
      LOCATION_PRODUCT_IDS,
      productPurchasesReportParameters.locationProductIds.toTypedArray()
    ).apply(context)

    AnyListFilterStrategy(
      "$LOCATION_PRODUCT_ALIAS.category_id",
      CATEGORY_IDS,
      productPurchasesReportParameters.categoryIds.toTypedArray()
    ).apply(context)

    return context
  }

  private fun buildReceivedDeliveryPredicate(
    deliveredRange: ProductPurchasesDeliveredRange,
    supplierIds: List<UUID>
  ): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("$DELIVERY_ALIAS.status = ANY(:$APPLIED_DELIVERY_STATUSES)")
    context.params[APPLIED_DELIVERY_STATUSES] = PurchaseDeliveryStatus.entries
      .filter { it.isAppliedToStock() }
      .map { it.code }
      .toTypedArray()

    DateRangeFilterStrategy(
      "$DELIVERY_ALIAS.delivered_at",
      DELIVERED_FROM,
      deliveredRange.fromInclusive,
      DELIVERED_BEFORE,
      deliveredRange.beforeExclusive
    ).apply(context)

    AnyListFilterStrategy(
      "$PURCHASE_ALIAS.supplier_id",
      SUPPLIER_IDS,
      supplierIds.toTypedArray()
    ).apply(context)

    return context
  }

  // Each line is rounded to money scale before it is summed, so a total equals the sum of the line totals shown.
  fun buildSummaryQuery(predicate: QueryBuilderContext): SqlQuery {
    val sql = """
      SELECT
        $PURCHASE_LINE_ALIAS.location_product_id AS location_product_id,
        $LOCATION_PRODUCT_ALIAS.category_id AS category_id,
        COUNT($DELIVERY_LINE_ALIAS.id) AS delivery_line_count,
        SUM($DELIVERY_LINE_ALIAS.user_provided_quantity_delivered * $DELIVERY_LINE_ALIAS.storage_units_per_user_provided_unit) AS storage_quantity,
        SUM(ROUND($DELIVERY_LINE_ALIAS.user_provided_quantity_delivered * $DELIVERY_LINE_ALIAS.unit_cost, :$MONEY_SCALE)) AS total_cost
      $FROM_AND_JOINS
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY $PURCHASE_LINE_ALIAS.location_product_id, $LOCATION_PRODUCT_ALIAS.category_id
    """.trimIndent()

    return SqlQuery(
      sql,
      predicate.params + (MONEY_SCALE to Decimals.MoneyScale.scale),
      ProductPurchasesReportQueryMetadata("product_purchases_report_summary")
    )
  }

  fun buildDeliveryLinesQuery(
    locationProductId: UUID,
    productPurchasesDeliveryLinesParameters: ProductPurchasesDeliveryLinesParameters,
    deliveredRange: ProductPurchasesDeliveredRange,
    cursor: KeysetSearchCursor?
  ): SqlQuery {
    val context = buildReceivedDeliveryPredicate(deliveredRange, productPurchasesDeliveryLinesParameters.supplierIds)
    context.whereClauses.add("$PURCHASE_LINE_ALIAS.location_product_id = :$LOCATION_PRODUCT_ID")
    context.params[LOCATION_PRODUCT_ID] = locationProductId

    if (cursor != null) {
      context.whereClauses.add(
        "($DELIVERY_ALIAS.delivered_at, $DELIVERY_LINE_ALIAS.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
      )
      context.params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
      context.params[QueryParameterNames.CURSOR_ID] = cursor.id
    }

    val sql = """
      SELECT
        $DELIVERY_LINE_ALIAS.id AS delivery_line_id,
        $PURCHASE_ALIAS.reference_number AS purchase_reference_number,
        $DELIVERY_ALIAS.reference_number AS delivery_reference_number,
        $DELIVERY_ALIAS.delivered_at AS delivered_at,
        $PURCHASE_ALIAS.supplier_id AS supplier_id,
        $DELIVERY_LINE_ALIAS.unit_id AS unit_id,
        $DELIVERY_LINE_ALIAS.user_provided_quantity_delivered AS user_provided_quantity_delivered,
        $DELIVERY_LINE_ALIAS.storage_units_per_user_provided_unit AS storage_units_per_user_provided_unit,
        $DELIVERY_LINE_ALIAS.unit_cost AS unit_cost,
        $DELIVERY_LINE_ALIAS.status AS status
      $FROM_AND_JOINS
      WHERE ${context.whereClauses.joinToString(" AND ")}
      ORDER BY $DELIVERY_ALIAS.delivered_at DESC, $DELIVERY_LINE_ALIAS.id DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()

    return SqlQuery(sql, context.params, ProductPurchasesReportQueryMetadata("product_purchases_report_delivery_lines"))
  }
}
