package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ProductSalesLineRawRow(
  val saleLineId: UUID,
  val saleReferenceNumber: String,
  val dateSold: OffsetDateTime,
  val unitId: UUID,
  val userProvidedQuantity: BigDecimal,
  val storageUnitsPerUserProvidedUnit: Long,
  val lineTotal: BigDecimal,
  val cogsTotal: BigDecimal?
)

data class ProductSalesLineAdjustmentRawRow(
  val saleLineId: UUID,
  val adjustmentReasonId: UUID,
  val direction: AdjustmentDirection,
  val calculatedAmount: BigDecimal
)

// Discount and surcharge amounts are unsigned magnitudes, as stored on sale_adjustment.
data class ProductSalesSummaryRawRow(
  val locationProductId: UUID,
  val lineCount: Long,
  val storageQuantity: BigDecimal,
  val grossSales: BigDecimal,
  val cogsTotal: BigDecimal,
  val discountMagnitude: BigDecimal,
  val surchargeMagnitude: BigDecimal
) {

  operator fun plus(other: ProductSalesSummaryRawRow): ProductSalesSummaryRawRow {
    require(locationProductId == other.locationProductId) { "Only rows of one product can be added" }
    return ProductSalesSummaryRawRow(
      locationProductId = locationProductId,
      lineCount = lineCount + other.lineCount,
      storageQuantity = storageQuantity + other.storageQuantity,
      grossSales = grossSales + other.grossSales,
      cogsTotal = cogsTotal + other.cogsTotal,
      discountMagnitude = discountMagnitude + other.discountMagnitude,
      surchargeMagnitude = surchargeMagnitude + other.surchargeMagnitude
    )
  }
}

data class ProductSalesDailyRawRow(
  val soldOn: LocalDate,
  val summary: ProductSalesSummaryRawRow
)
