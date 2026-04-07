package me.ezra_home.retail_software_solution.locations.business.delivery.api

import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveredLineDto(
  val deliveryLineId: UUID,
  val locationProductId: UUID,
  val quantityDelivered: BigDecimal,
  val unitCost: BigDecimal
)
