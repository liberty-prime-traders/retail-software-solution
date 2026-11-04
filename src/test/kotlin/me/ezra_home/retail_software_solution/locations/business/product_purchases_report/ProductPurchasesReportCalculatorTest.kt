package me.ezra_home.retail_software_solution.locations.business.product_purchases_report

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ProductPurchasesReportCalculatorTest {

  private val locationProductId = UUID.randomUUID()
  private val categoryId = UUID.randomUUID()
  private val pieceUnitId = UUID.randomUUID()
  private val dozenUnitId = UUID.randomUUID()
  private val identity = LocationProductSummaryDto(locationProductId, "LP000001", UUID.randomUUID(), "Rice", "Grains", dozenUnitId)
  private val breakdown = QuantityBreakdown.forBaseUnit(
    listOf(
      BreakdownUnit(pieceUnitId, "Piece", 1, pieceUnitId, OffsetDateTime.now()),
      BreakdownUnit(dozenUnitId, "Dozen", 12, pieceUnitId, OffsetDateTime.now())
    ),
    dozenUnitId
  )

  @Test
  fun `row cost per base unit comes from summed totals and summed storage quantity`() {
    val row = ProductPurchasesReportCalculator.toRowDto(
      productPurchasesSummaryRawRow = ProductPurchasesSummaryRawRow(
        locationProductId = locationProductId,
        categoryId = categoryId,
        deliveryLineCount = 2,
        storageQuantity = BigDecimal("24"),
        totalCost = BigDecimal("60.00")
      ),
      locationProductSummaryDto = identity,
      categoryName = "Grocery",
      quantityBreakdown = breakdown,
      storageUnitsPerBaseUnit = 12
    )

    assertEquals("Grocery", row.categoryName)
    assertEquals(2L, row.deliveryLineCount)
    assertEquals(0, BigDecimal("60.00").compareTo(row.totalCost))
    assertEquals(DisplayFormatters.formatCurrency(BigDecimal("30")), row.averageCostPerBaseUnit)
  }

  @Test
  fun `row received quantity is rendered as a breakdown across unit levels`() {
    val row = ProductPurchasesReportCalculator.toRowDto(
      productPurchasesSummaryRawRow = ProductPurchasesSummaryRawRow(
        locationProductId = locationProductId,
        categoryId = categoryId,
        deliveryLineCount = 1,
        storageQuantity = BigDecimal("25"),
        totalCost = BigDecimal("50.00")
      ),
      locationProductSummaryDto = identity,
      categoryName = "Grocery",
      quantityBreakdown = breakdown,
      storageUnitsPerBaseUnit = 12
    )

    assertEquals("2 Dozens 1 Piece", row.receivedQuantityBreakdown)
  }

  @Test
  fun `row average cost is null when nothing was delivered in storage units`() {
    val row = ProductPurchasesReportCalculator.toRowDto(
      productPurchasesSummaryRawRow = ProductPurchasesSummaryRawRow(locationProductId, categoryId, 1, BigDecimal.ZERO, BigDecimal.ZERO),
      locationProductSummaryDto = identity,
      categoryName = "Grocery",
      quantityBreakdown = breakdown,
      storageUnitsPerBaseUnit = 12
    )

    assertNull(row.averageCostPerBaseUnit)
  }

  @Test
  fun `delivery line total is rounded to money scale and base quantity uses the frozen ratio`() {
    val line = ProductPurchasesReportCalculator.toDeliveryLineDto(
      productPurchasesDeliveryLineRawRow = ProductPurchasesDeliveryLineRawRow(
        id = UUID.randomUUID(),
        purchaseReferenceNumber = "PO000001",
        deliveryReferenceNumber = "DL000001",
        deliveredAt = OffsetDateTime.parse("2026-10-01T09:00:00+00:00"),
        supplierId = UUID.randomUUID(),
        unitId = dozenUnitId,
        userProvidedQuantityDelivered = BigDecimal("3"),
        storageUnitsPerUserProvidedUnit = 12,
        unitCost = BigDecimal("10.005"),
        status = PurchaseDeliveryLineStatus.ACTIVE
      ),
      supplierName = "Kisumu Traders",
      quantityBreakdown = breakdown,
      storageUnitsPerBaseUnit = 12
    )

    assertEquals(0, BigDecimal("3").compareTo(line.baseQuantity))
    assertEquals(0, BigDecimal("10.005").compareTo(line.unitCost))
    assertEquals(0, BigDecimal("30.02").compareTo(line.lineTotal))
  }
}
