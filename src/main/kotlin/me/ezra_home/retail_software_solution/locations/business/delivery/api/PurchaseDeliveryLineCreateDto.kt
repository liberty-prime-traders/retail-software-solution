package me.ezra_home.retail_software_solution.locations.business.delivery.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveryLineCreateDto(
  val purchaseLineId: UUID,
  @param:JsonProperty("quantityDelivered")
  val userProvidedQuantityDelivered: BigDecimal,
  val unitId: UUID,
  val unitCost: BigDecimal
) {
  init {
    Decimals.requireMaxDecimalPlaces(userProvidedQuantityDelivered, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
  }
}
