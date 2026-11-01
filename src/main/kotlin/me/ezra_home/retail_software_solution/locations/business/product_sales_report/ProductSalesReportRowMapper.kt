package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object ProductSalesReportRowMapper {

  fun lineFromTuple(tuple: Tuple): ProductSalesLineRawRow = ProductSalesLineRawRow(
    saleLineId = tuple.get("sale_line_id", UUID::class.java),
    saleReferenceNumber = tuple.get("reference_number", String::class.java),
    dateSold = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("date_sold", Instant::class.java))),
    unitId = tuple.get("unit_id", UUID::class.java),
    userProvidedQuantity = tuple.get("user_provided_quantity", BigDecimal::class.java),
    storageUnitsPerUserProvidedUnit = tuple.get("storage_units_per_user_provided_unit", Number::class.java).toLong(),
    lineTotal = tuple.get("line_total", BigDecimal::class.java),
    cogsTotal = tuple.get("cogs_total", BigDecimal::class.java)
  )

  fun adjustmentFromTuple(tuple: Tuple): ProductSalesLineAdjustmentRawRow = ProductSalesLineAdjustmentRawRow(
    saleLineId = tuple.get("sale_line_id", UUID::class.java),
    adjustmentReasonId = tuple.get("adjustment_reason_id", UUID::class.java),
    direction = AdjustmentDirection.entries.first { it.code == tuple.get("direction", String::class.java) },
    calculatedAmount = tuple.get("calculated_amount", BigDecimal::class.java)
  )

  fun dailySummaryFromTuple(tuple: Tuple): ProductSalesDailyRawRow = ProductSalesDailyRawRow(
    soldOn = LocalDate.parse(tuple.get("sold_on", String::class.java)),
    summary = summaryFromTuple(tuple)
  )

  fun summaryFromTuple(tuple: Tuple): ProductSalesSummaryRawRow = ProductSalesSummaryRawRow(
    locationProductId = tuple.get("location_product_id", UUID::class.java),
    lineCount = tuple.get("line_count", Number::class.java).toLong(),
    storageQuantity = tuple.get("storage_quantity", BigDecimal::class.java),
    grossSales = tuple.get("gross_sales", BigDecimal::class.java),
    cogsTotal = tuple.get("cogs_total", BigDecimal::class.java),
    discountMagnitude = tuple.get("discount_magnitude", BigDecimal::class.java),
    surchargeMagnitude = tuple.get("surcharge_magnitude", BigDecimal::class.java)
  )
}
