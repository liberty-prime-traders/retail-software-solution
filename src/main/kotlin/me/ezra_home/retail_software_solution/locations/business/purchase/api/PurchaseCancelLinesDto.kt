package me.ezra_home.retail_software_solution.locations.business.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

// unitId is not stored; absent means the line's ordered unit
data class PurchaseCancelLinesDto(
  val purchaseLineId: UUID,
  @param:JsonProperty("quantityCanceled")
  val userProvidedQuantityCanceled: BigDecimal,
  val unitId: UUID? = null
) {
  init {
    Decimals.requireMaxDecimalPlaces(userProvidedQuantityCanceled, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
  }
}
