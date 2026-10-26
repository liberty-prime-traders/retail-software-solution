package me.ezra_home.retail_software_solution.locations.business.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

// unitId is not stored; absent means the line's ordered unit
data class PurchaseCancelLinesDto(
  val purchaseLineId: UUID,
  @param:JsonProperty("quantityCanceled")
  val userProvidedQuantityCanceled: PositiveUserProvidedQuantity,
  val unitId: UUID? = null
)
