package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesLineAdjustmentDto
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesLineDto
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object ProductSalesReportCalculator {

  private val ONE_HUNDRED = BigDecimal(100)

  fun signedAmount(direction: AdjustmentDirection, calculatedAmount: BigDecimal): BigDecimal = when (direction) {
    AdjustmentDirection.DISCOUNT -> calculatedAmount.negate()
    AdjustmentDirection.SURCHARGE -> calculatedAmount
    AdjustmentDirection.BOTH -> error("A sale adjustment is always a DISCOUNT or a SURCHARGE, never BOTH")
  }

  fun toLineDto(
    rawRow: ProductSalesLineRawRow,
    adjustments: List<ProductSalesLineAdjustmentDto>,
    quantityEntered: String,
    storageUnitsPerBaseUnit: Long
  ): ProductSalesLineDto {
    val grossSales = Decimals.Scale4.round(rawRow.lineTotal)
    val netSales = Decimals.Scale4.round(grossSales + adjustments.sumOf { it.amount })
    val cogs = Decimals.Scale4.round(rawRow.cogsTotal ?: BigDecimal.ZERO)
    val grossProfit = netSales - cogs

    return ProductSalesLineDto(
      id = rawRow.saleLineId,
      saleReferenceNumber = rawRow.saleReferenceNumber,
      dateSold = rawRow.dateSold,
      quantityEntered = quantityEntered,
      baseQuantity = BaseUnitDisplay.baseQuantity(
        rawRow.userProvidedQuantity,
        rawRow.storageUnitsPerUserProvidedUnit,
        storageUnitsPerBaseUnit,
        RoundingMode.HALF_UP
      ),
      grossSales = grossSales,
      netSales = netSales,
      cogs = cogs,
      grossProfit = grossProfit,
      marginPercent = percentOf(grossProfit, netSales),
      adjustments = adjustments
    )
  }

  fun sumByProduct(rawRows: Iterable<ProductSalesSummaryRawRow>): List<ProductSalesSummaryRawRow> {
    val totalsByLocationProductId = HashMap<UUID, ProductSalesSummaryRawRow>()
    rawRows.forEach { totalsByLocationProductId.merge(it.locationProductId, it) { total, row -> total + row } }
    return totalsByLocationProductId.values.toList()
  }

  fun emptySummaryRawRow(locationProductId: UUID): ProductSalesSummaryRawRow = ProductSalesSummaryRawRow(
    locationProductId = locationProductId,
    lineCount = 0,
    storageQuantity = BigDecimal.ZERO,
    grossSales = BigDecimal.ZERO,
    cogsTotal = BigDecimal.ZERO,
    discountMagnitude = BigDecimal.ZERO,
    surchargeMagnitude = BigDecimal.ZERO
  )

  fun toSummaryDto(
    rawRow: ProductSalesSummaryRawRow,
    locationProductSummaryDto: LocationProductSummaryDto,
    quantityBreakdown: QuantityBreakdown,
    baseUnitName: String,
    storageUnitsPerBaseUnit: Long
  ): ProductSalesSummaryDto {
    val baseQuantity = BaseUnitDisplay.storageQuantityInBaseUnit(rawRow.storageQuantity, storageUnitsPerBaseUnit, RoundingMode.HALF_UP)
    val grossSales = Decimals.Scale4.round(rawRow.grossSales)
    val discountTotal = Decimals.Scale4.round(rawRow.discountMagnitude.negate())
    val surchargeTotal = Decimals.Scale4.round(rawRow.surchargeMagnitude)
    val netSales = grossSales + discountTotal + surchargeTotal
    val cogs = Decimals.Scale4.round(rawRow.cogsTotal)
    val grossProfit = netSales - cogs

    return ProductSalesSummaryDto(
      locationProductId = rawRow.locationProductId,
      orgProductId = locationProductSummaryDto.orgProductId,
      productName = locationProductSummaryDto.productName,
      productGroupName = locationProductSummaryDto.productGroupName,
      baseUnitName = baseUnitName,
      lineCount = rawRow.lineCount,
      baseQuantity = baseQuantity,
      baseQuantityText = quantityBreakdown.renderUserProvided(baseQuantity, locationProductSummaryDto.baseUnitId),
      grossSales = grossSales,
      discountTotal = discountTotal,
      surchargeTotal = surchargeTotal,
      netSales = netSales,
      cogs = cogs,
      grossProfit = grossProfit,
      marginPercent = percentOf(grossProfit, netSales),
      marginBeforeAdjustmentsPercent = percentOf(grossSales - cogs, grossSales),
      adjustmentsPercent = percentOf(grossSales - netSales, grossSales),
      avgListPrice = BaseUnitDisplay.pricePerBaseUnit(grossSales, rawRow.storageQuantity, storageUnitsPerBaseUnit),
      avgSellingPrice = BaseUnitDisplay.pricePerBaseUnit(netSales, rawRow.storageQuantity, storageUnitsPerBaseUnit),
      avgCost = BaseUnitDisplay.pricePerBaseUnit(cogs, rawRow.storageQuantity, storageUnitsPerBaseUnit),
      profitPerBaseUnit = BaseUnitDisplay.pricePerBaseUnit(grossProfit, rawRow.storageQuantity, storageUnitsPerBaseUnit)
    )
  }

  fun percentOf(numerator: BigDecimal, denominator: BigDecimal): BigDecimal? =
    if (denominator.signum() == 0) null else Decimals.PercentDisplayScale.divide(numerator.multiply(ONE_HUNDRED), denominator)
}
