package me.ezra_home.retail_software_solution.locations.business.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.locations.business.purchase.HasLocationProduct
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class PurchaseLineCreateDto(
  override val locationProductId: UUID,
  @param:JsonProperty("quantityOrdered")
  val userProvidedQuantityOrdered: PositiveUserProvidedQuantity,
  val unitId: UUID,
  val unitCost: Money
) : HasLocationProduct
