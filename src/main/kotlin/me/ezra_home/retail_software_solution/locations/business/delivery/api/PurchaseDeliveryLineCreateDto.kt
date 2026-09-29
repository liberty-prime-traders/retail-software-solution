package me.ezra_home.retail_software_solution.locations.business.delivery.api

import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveryLineCreateDto(
  val purchaseLineId: UUID,
  val quantityDelivered: BigDecimal,
  val unitId: UUID,
  val unitCost: BigDecimal
)
