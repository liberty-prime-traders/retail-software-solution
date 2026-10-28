package me.ezra_home.retail_software_solution.locations.business.delivery.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class PurchaseDeliveryLineCreateDto(
  val purchaseLineId: UUID,
  @param:JsonProperty("quantityDelivered")
  val userProvidedQuantityDelivered: PositiveUserProvidedQuantity,
  val unitId: UUID,
  val unitCost: PositiveMoney
)
