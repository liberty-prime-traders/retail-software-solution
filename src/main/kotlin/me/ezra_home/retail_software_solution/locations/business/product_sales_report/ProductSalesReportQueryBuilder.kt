package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.model.TableNames
import me.ezra_home.retail_software_solution.util.queries.AnyListFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.DateRangeFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import java.util.UUID

object ProductSalesReportQueryBuilder {

  private const val SALE_ALIAS = "s"
  private const val SALE_LINE_ALIAS = "sl"
  private const val SALE_ADJUSTMENT_ALIAS = "sa"
  private const val LINE_ADJUSTMENTS_ALIAS = "adj"

  private const val DATE_SOLD_FROM = "dateSoldFrom"
  private const val DATE_SOLD_BEFORE = "dateSoldBefore"
  private const val LOCATION_PRODUCT_IDS = "locationProductIds"
  private const val SALE_LINE_IDS = "saleLineIds"
  private const val CONFIRMED_STATUS = "confirmedStatus"
  private const val DISCOUNT_DIRECTION = "discountDirection"
  private const val SURCHARGE_DIRECTION = "surchargeDirection"
  private const val ORGANIZATION_ZONE = "organizationZone"

  fun buildPredicate(locationProductIds: List<UUID>, dateRange: DateSoldRange): QueryBuilderContext {
    val context = buildAllProductsPredicate(dateRange)

    AnyListFilterStrategy(
      "$SALE_LINE_ALIAS.location_product_id",
      LOCATION_PRODUCT_IDS,
      locationProductIds.toTypedArray()
    ).apply(context)

    return context
  }

  fun buildAllProductsPredicate(dateRange: DateSoldRange): QueryBuilderContext {
    val context = QueryBuilderContext()
    context.whereClauses.add("$SALE_ALIAS.status = :$CONFIRMED_STATUS")
    context.params[CONFIRMED_STATUS] = SaleStatus.CONFIRMED.code

    DateRangeFilterStrategy(
      "$SALE_ALIAS.date_sold",
      DATE_SOLD_FROM,
      dateRange.fromInclusive,
      DATE_SOLD_BEFORE,
      dateRange.beforeExclusive
    ).apply(context)

    return context
  }

  // (date_sold, id) is a total order here: the confirmed-status predicate plus the date_sold range
  // bounds exclude every row where date_sold is null.
  fun buildLinesQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
    val whereClauses = predicate.whereClauses.toMutableList()
    val params = predicate.params.toMutableMap()

    if (cursor != null) {
      whereClauses.add(
        "($SALE_ALIAS.date_sold, $SALE_LINE_ALIAS.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
      )
      params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
      params[QueryParameterNames.CURSOR_ID] = cursor.id
    }

    val sql = """
      SELECT
        $SALE_LINE_ALIAS.id AS sale_line_id,
        $SALE_ALIAS.reference_number AS reference_number,
        $SALE_ALIAS.date_sold AS date_sold,
        $SALE_LINE_ALIAS.unit_id AS unit_id,
        $SALE_LINE_ALIAS.user_provided_quantity AS user_provided_quantity,
        $SALE_LINE_ALIAS.storage_units_per_user_provided_unit AS storage_units_per_user_provided_unit,
        $SALE_LINE_ALIAS.line_total AS line_total,
        $SALE_LINE_ALIAS.cogs_total AS cogs_total
      FROM ${TableNames.SALE_LINE} $SALE_LINE_ALIAS
      JOIN ${TableNames.SALE} $SALE_ALIAS ON $SALE_ALIAS.id = $SALE_LINE_ALIAS.sale_id
      WHERE ${whereClauses.joinToString(" AND ")}
      ORDER BY $SALE_ALIAS.date_sold DESC, $SALE_LINE_ALIAS.id DESC
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()

    return SqlQuery(sql, params, ProductSalesReportQueryMetadata("product_sales_report_lines"))
  }

  fun buildLineAdjustmentsQuery(saleLineIds: List<UUID>): SqlQuery {
    val sql = """
      SELECT
        $SALE_ADJUSTMENT_ALIAS.sale_line_id AS sale_line_id,
        $SALE_ADJUSTMENT_ALIAS.adjustment_reason_id AS adjustment_reason_id,
        $SALE_ADJUSTMENT_ALIAS.direction AS direction,
        $SALE_ADJUSTMENT_ALIAS.calculated_amount AS calculated_amount
      FROM ${TableNames.SALE_ADJUSTMENT} $SALE_ADJUSTMENT_ALIAS
      WHERE $SALE_ADJUSTMENT_ALIAS.sale_line_id = ANY(:$SALE_LINE_IDS)
      ORDER BY $SALE_ADJUSTMENT_ALIAS.sale_line_id, $SALE_ADJUSTMENT_ALIAS.created_on, $SALE_ADJUSTMENT_ALIAS.id
    """.trimIndent()

    return SqlQuery(
      sql,
      mapOf(SALE_LINE_IDS to saleLineIds.toTypedArray()),
      ProductSalesReportQueryMetadata("product_sales_report_line_adjustments")
    )
  }

  fun buildSummaryQuery(predicate: QueryBuilderContext): SqlQuery =
    summaryQuery(predicate, soldOnSelect = "", groupBy = "", queryName = "product_sales_report_summary")

  // sold_on is the sale's calendar day in the organization zone, as yyyy-MM-dd text so the driver's date type never matters.
  fun buildDailySummaryQuery(predicate: QueryBuilderContext, organizationZone: String): SqlQuery {
    val dailyPredicate = QueryBuilderContext()
    dailyPredicate.whereClauses.addAll(predicate.whereClauses)
    dailyPredicate.params.putAll(predicate.params)
    dailyPredicate.params[ORGANIZATION_ZONE] = organizationZone
    return summaryQuery(
      dailyPredicate,
      soldOnSelect = "to_char($SALE_ALIAS.date_sold AT TIME ZONE CAST(:$ORGANIZATION_ZONE AS text), 'YYYY-MM-DD') AS sold_on,",
      groupBy = "sold_on, ",
      queryName = "product_sales_report_daily_summary"
    )
  }

  // The lateral subquery is an ungrouped aggregate, so it yields exactly one row per sale line and the
  // outer SUMs of line_total, cogs_total and quantity are never multiplied by the adjustment count.
  private fun summaryQuery(predicate: QueryBuilderContext, soldOnSelect: String, groupBy: String, queryName: String): SqlQuery {
    val params = predicate.params.toMutableMap()
    params[DISCOUNT_DIRECTION] = AdjustmentDirection.DISCOUNT.code
    params[SURCHARGE_DIRECTION] = AdjustmentDirection.SURCHARGE.code

    val sql = """
      SELECT
        $soldOnSelect
        $SALE_LINE_ALIAS.location_product_id AS location_product_id,
        COUNT(*) AS line_count,
        COALESCE(SUM($SALE_LINE_ALIAS.user_provided_quantity * $SALE_LINE_ALIAS.storage_units_per_user_provided_unit), 0) AS storage_quantity,
        COALESCE(SUM($SALE_LINE_ALIAS.line_total), 0) AS gross_sales,
        COALESCE(SUM($SALE_LINE_ALIAS.cogs_total), 0) AS cogs_total,
        COALESCE(SUM($LINE_ADJUSTMENTS_ALIAS.discount_magnitude), 0) AS discount_magnitude,
        COALESCE(SUM($LINE_ADJUSTMENTS_ALIAS.surcharge_magnitude), 0) AS surcharge_magnitude
      FROM ${TableNames.SALE_LINE} $SALE_LINE_ALIAS
      JOIN ${TableNames.SALE} $SALE_ALIAS ON $SALE_ALIAS.id = $SALE_LINE_ALIAS.sale_id
      LEFT JOIN LATERAL (
        SELECT
          COALESCE(SUM(CASE WHEN $SALE_ADJUSTMENT_ALIAS.direction = :$DISCOUNT_DIRECTION
                            THEN $SALE_ADJUSTMENT_ALIAS.calculated_amount ELSE 0 END), 0) AS discount_magnitude,
          COALESCE(SUM(CASE WHEN $SALE_ADJUSTMENT_ALIAS.direction = :$SURCHARGE_DIRECTION
                            THEN $SALE_ADJUSTMENT_ALIAS.calculated_amount ELSE 0 END), 0) AS surcharge_magnitude
        FROM ${TableNames.SALE_ADJUSTMENT} $SALE_ADJUSTMENT_ALIAS
        WHERE $SALE_ADJUSTMENT_ALIAS.sale_line_id = $SALE_LINE_ALIAS.id
      ) $LINE_ADJUSTMENTS_ALIAS ON TRUE
      WHERE ${predicate.whereClauses.joinToString(" AND ")}
      GROUP BY $groupBy$SALE_LINE_ALIAS.location_product_id
    """.trimIndent()

    return SqlQuery(sql, params, ProductSalesReportQueryMetadata(queryName))
  }
}
