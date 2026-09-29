package me.ezra_home.retail_software_solution.locations.business.purchase.search

import java.math.BigDecimal
import java.util.UUID

data class PurchasePaymentStatusSummaryRawRow(
  val paymentStatusCode: String,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalPaid: BigDecimal
)

data class PurchaseSupplierSummaryRawRow(
  val supplierId: UUID,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalPaid: BigDecimal
)
