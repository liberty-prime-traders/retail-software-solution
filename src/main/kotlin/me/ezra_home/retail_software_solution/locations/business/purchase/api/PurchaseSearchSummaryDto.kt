package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.math.BigDecimal
import java.util.UUID

data class PurchasePaymentStatusSummaryDto(
  val paymentStatus: PaymentStatus,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalPaid: BigDecimal
) {
  val totalOutstanding: BigDecimal = totalOrdered - totalPaid
}

data class PurchaseSupplierSummaryDto(
  val supplierId: UUID,
  val supplierName: String,
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalPaid: BigDecimal
) {
  val totalOutstanding: BigDecimal = totalOrdered - totalPaid
}

data class PurchaseSearchSummaryDto(
  val purchaseCount: Long,
  val totalOrdered: BigDecimal,
  val totalPaid: BigDecimal,
  val byPaymentStatus: List<PurchasePaymentStatusSummaryDto>,
  val bySupplier: List<PurchaseSupplierSummaryDto>?
) {
  val totalOutstanding: BigDecimal = totalOrdered - totalPaid
}
