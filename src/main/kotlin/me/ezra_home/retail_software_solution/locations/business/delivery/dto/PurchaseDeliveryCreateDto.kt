package me.ezra_home.retail_software_solution.locations.business.delivery.dto

import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseDeliveryCreateDto(
  val purchaseId: UUID,
  val deliveredAt: OffsetDateTime?,
  val notes: String?,
  val lines: List<PurchaseDeliveryLineCreateDto>
)
