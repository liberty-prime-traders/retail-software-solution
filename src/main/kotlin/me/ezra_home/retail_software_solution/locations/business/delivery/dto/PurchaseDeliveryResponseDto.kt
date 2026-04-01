package me.ezra_home.retail_software_solution.locations.business.delivery.dto

import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseDeliveryStatus
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseDeliveryResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseId: UUID,
  val status: PurchaseDeliveryStatus,
  val deliveredAt: OffsetDateTime?,
  val notes: String?,
  val lines: List<PurchaseDeliveryLineResponseDto>
)
