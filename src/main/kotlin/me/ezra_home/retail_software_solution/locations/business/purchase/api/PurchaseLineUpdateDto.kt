package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineUpdateDto(
  val id: UUID,
  val quantityOrdered: BigDecimal,
  val unitId: UUID,
  val unitCost: BigDecimal
) : Serializable
