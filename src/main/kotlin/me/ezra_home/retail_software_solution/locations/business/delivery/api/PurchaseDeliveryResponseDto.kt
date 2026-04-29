package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryStatus
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PurchaseDeliveryResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseId: UUID,
  val status: PurchaseDeliveryStatus,
  val deliveredAt: Instant,
  val notes: String?,
  val lines: List<PurchaseDeliveryLineResponseDto>,
  val deliveryTotal: BigDecimal = lines.sumOf { Decimals.multiplyScale4(it.quantityDelivered, it.unitCost) }
)

