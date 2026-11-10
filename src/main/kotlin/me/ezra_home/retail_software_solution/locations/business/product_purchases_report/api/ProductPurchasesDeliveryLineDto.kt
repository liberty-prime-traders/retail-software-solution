package me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ProductPurchasesDeliveryLineDto(
  val id: UUID,
  val purchaseReferenceNumber: String,
  val deliveryReferenceNumber: String,
  val deliveredAt: OffsetDateTime,
  val supplierName: String,
  val quantityDeliveredText: String,
  val baseQuantity: BigDecimal,
  val unitCost: BigDecimal,
  val lineTotal: BigDecimal,
  val status: PurchaseDeliveryLineStatus
)
