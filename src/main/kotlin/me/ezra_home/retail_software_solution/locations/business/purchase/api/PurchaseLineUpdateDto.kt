package me.ezra_home.retail_software_solution.locations.business.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import java.util.UUID

data class PurchaseLineUpdateDto(
  val id: UUID,
  @param:JsonProperty("quantityOrdered")
  val userProvidedQuantityOrdered: UserProvidedQuantity,
  val unitId: UUID,
  val unitCost: Money
)
