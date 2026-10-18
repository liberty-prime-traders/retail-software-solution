package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.locations.business.purchase.HasLocationProduct
import me.ezra_home.retail_software_solution.locations.business.purchase.HasUnitCost
import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineCreateDto(
  override val locationProductId: UUID,
  @param:JsonProperty("quantityOrdered")
  val userProvidedQuantityOrdered: BigDecimal,
  val unitId: UUID,
  override val unitCost: BigDecimal
) : HasLocationProduct, HasUnitCost {
  init {
    Decimals.requireMaxDecimalPlaces(userProvidedQuantityOrdered, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
  }
}
