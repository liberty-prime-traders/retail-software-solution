package me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api

import java.math.BigDecimal
import java.util.UUID

data class ProductPurchasesReportRowDto(
  val locationProductId: UUID,
  val referenceNumber: String,
  val productName: String,
  val productGroupName: String,
  val categoryName: String,
  val deliveryLineCount: Long,
  val receivedQuantityBreakdown: String,
  val totalCost: BigDecimal,
  val averageCostPerBaseUnit: String?
)
