package me.ezra_home.retail_software_solution.locations.business.product_sales_report

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.api.ProductSalesLineAdjustmentDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ProductSalesReportCalculatorTest {

  private val locationProductId = UUID.randomUUID()
  private val baseUnitId = UUID.randomUUID()
  private val identity = LocationProductSummaryDto(locationProductId, "LP000001", UUID.randomUUID(), "Rice", "Grains", baseUnitId)
  private val breakdown = ProductSalesReportTestSummaries.pieceBreakdown(baseUnitId)

  private fun summaryRow(
    storageQuantity: String,
    grossSales: String,
    cogsTotal: String,
    discountMagnitude: String = "0",
    surchargeMagnitude: String = "0"
  ) = ProductSalesSummaryRawRow(
    locationProductId = locationProductId,
    lineCount = 1,
    storageQuantity = BigDecimal(storageQuantity),
    grossSales = BigDecimal(grossSales),
    cogsTotal = BigDecimal(cogsTotal),
    discountMagnitude = BigDecimal(discountMagnitude),
    surchargeMagnitude = BigDecimal(surchargeMagnitude)
  )

  @Test
  fun `summary derives signed adjustments, net, profit, margins and per-unit figures from summed totals`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = "24", grossSales = "100", cogsTotal = "60", discountMagnitude = "10", surchargeMagnitude = "5"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    )

    assertEquals(BigDecimal("-10.0000"), summary.discountTotal)
    assertEquals(BigDecimal("5.0000"), summary.surchargeTotal)
    assertEquals(BigDecimal("95.0000"), summary.netSales)
    assertEquals(BigDecimal("35.0000"), summary.grossProfit)
    assertEquals(BigDecimal("36.8"), summary.marginPercent)
    assertEquals(BigDecimal("40.0"), summary.marginBeforeAdjustmentsPercent)
    assertEquals(BigDecimal("5.0"), summary.adjustmentsPercent)
    assertEquals(BigDecimal("24.0000"), summary.baseQuantity)
    assertEquals(BigDecimal("4.1667"), summary.avgListPrice)
    assertEquals(BigDecimal("3.9583"), summary.avgSellingPrice)
    assertEquals(BigDecimal("2.5000"), summary.avgCost)
    assertEquals(BigDecimal("1.4583"), summary.profitPerBaseUnit)
  }

  @Test
  fun `summary carries the product identity`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = "1", grossSales = "10", cogsTotal = "5"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    )

    assertEquals(identity.orgProductId, summary.orgProductId)
    assertNull(summary.rank)
    assertEquals("Rice", summary.productName)
    assertEquals("Grains", summary.productGroupName)
  }

  @Test
  fun `base quantity text pluralizes the base unit from the breakdown`() {
    fun textFor(storageQuantity: String) = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = storageQuantity, grossSales = "10", cogsTotal = "5"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    ).baseQuantityText

    assertEquals("1 Piece", textFor("1"))
    assertEquals("24 Pieces", textFor("24"))
    assertEquals("0 Pieces", textFor("0"))
  }

  @Test
  fun `summary converts the summed storage quantity once into the base unit`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = "30", grossSales = "130", cogsTotal = "60"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Carton",
      storageUnitsPerBaseUnit = 12
    )

    assertEquals(BigDecimal("2.5000"), summary.baseQuantity)
    assertEquals(BigDecimal("52.0000"), summary.avgListPrice)
  }

  @Test
  fun `a single piece sold from a 100000-piece base unit still prices per base unit`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = "1", grossSales = "5", cogsTotal = "3"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Pallet",
      storageUnitsPerBaseUnit = 100_000
    )

    assertEquals(BigDecimal("500000.0000"), summary.avgListPrice)
    assertEquals(BigDecimal("300000.0000"), summary.avgCost)
    assertEquals(BigDecimal("200000.0000"), summary.profitPerBaseUnit)
  }

  @Test
  fun `zero base quantity gives null per-unit figures`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      ProductSalesReportCalculator.emptySummaryRawRow(locationProductId),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    )

    assertNull(summary.avgListPrice)
    assertNull(summary.avgSellingPrice)
    assertNull(summary.avgCost)
    assertNull(summary.profitPerBaseUnit)
  }

  @Test
  fun `zero net sales gives null percentages even when cogs is positive`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = "3", grossSales = "50", cogsTotal = "20", discountMagnitude = "50"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    )

    assertEquals(BigDecimal("0.0000"), summary.netSales)
    assertEquals(BigDecimal("-20.0000"), summary.grossProfit)
    assertNull(summary.marginPercent)
    assertEquals(BigDecimal("60.0"), summary.marginBeforeAdjustmentsPercent)
    assertEquals(BigDecimal("100.0"), summary.adjustmentsPercent)
  }

  @Test
  fun `zero gross sales gives null before-adjustment percentages`() {
    val summary = ProductSalesReportCalculator.toSummaryDto(
      summaryRow(storageQuantity = "3", grossSales = "0", cogsTotal = "20"),
      locationProductSummaryDto = identity,
      quantityBreakdown = breakdown,
      baseUnitName = "Piece",
      storageUnitsPerBaseUnit = 1
    )

    assertNull(summary.marginBeforeAdjustmentsPercent)
    assertNull(summary.adjustmentsPercent)
  }

  @Test
  fun `loss-making line has negative profit and negative margin`() {
    val line = ProductSalesReportCalculator.toLineDto(
      lineRow(lineTotal = "80", cogsTotal = "100"),
      adjustments = emptyList(),
      quantityEntered = "1 Piece",
      storageUnitsPerBaseUnit = 1
    )

    assertEquals(BigDecimal("-20.0000"), line.grossProfit)
    assertEquals(BigDecimal("-25.0"), line.marginPercent)
  }

  @Test
  fun `line nets its signed adjustments and treats null cogs as zero`() {
    val line = ProductSalesReportCalculator.toLineDto(
      lineRow(lineTotal = "100", cogsTotal = null),
      adjustments = listOf(
        ProductSalesLineAdjustmentDto("Loyalty", AdjustmentDirection.DISCOUNT, BigDecimal("-10")),
        ProductSalesLineAdjustmentDto("Rush", AdjustmentDirection.SURCHARGE, BigDecimal("5"))
      ),
      quantityEntered = "2 Cartons",
      storageUnitsPerBaseUnit = 12
    )

    assertEquals(BigDecimal("95.0000"), line.netSales)
    assertEquals(BigDecimal("0.0000"), line.cogs)
    assertEquals(BigDecimal("2.0000"), line.baseQuantity)
  }

  @Test
  fun `adjustment amounts are signed by direction`() {
    assertEquals(BigDecimal("-10"), ProductSalesReportCalculator.signedAmount(AdjustmentDirection.DISCOUNT, BigDecimal("10")))
    assertEquals(BigDecimal("10"), ProductSalesReportCalculator.signedAmount(AdjustmentDirection.SURCHARGE, BigDecimal("10")))
  }

  private fun lineRow(lineTotal: String, cogsTotal: String?) = ProductSalesLineRawRow(
    saleLineId = UUID.randomUUID(),
    saleReferenceNumber = "SAL1",
    dateSold = OffsetDateTime.parse("2030-03-20T10:00:00Z"),
    unitId = UUID.randomUUID(),
    userProvidedQuantity = BigDecimal("2"),
    storageUnitsPerUserProvidedUnit = 12,
    lineTotal = BigDecimal(lineTotal),
    cogsTotal = cogsTotal?.let { BigDecimal(it) }
  )

  @Test
  fun `summing day rows of one product equals the totals of the whole range`() {
    val locationProductId = UUID.randomUUID()
    val otherLocationProductId = UUID.randomUUID()
    val firstDay = ProductSalesSummaryRawRow(locationProductId, 2, BigDecimal("3"), BigDecimal("100.5000"), BigDecimal("40"), BigDecimal("5"), BigDecimal("1"))
    val secondDay = ProductSalesSummaryRawRow(locationProductId, 1, BigDecimal("2"), BigDecimal("50.2500"), BigDecimal("20"), BigDecimal("0"), BigDecimal("2"))
    val otherProduct = ProductSalesSummaryRawRow(otherLocationProductId, 1, BigDecimal("1"), BigDecimal("9"), BigDecimal("4"), BigDecimal("0"), BigDecimal("0"))

    val totals = ProductSalesReportCalculator.sumByProduct(listOf(firstDay, otherProduct, secondDay)).associateBy { it.locationProductId }

    assertEquals(2, totals.size)
    assertEquals(
      ProductSalesSummaryRawRow(locationProductId, 3, BigDecimal("5"), BigDecimal("150.7500"), BigDecimal("60"), BigDecimal("5"), BigDecimal("3")),
      totals.getValue(locationProductId)
    )
    assertEquals(otherProduct, totals.getValue(otherLocationProductId))
  }
}
