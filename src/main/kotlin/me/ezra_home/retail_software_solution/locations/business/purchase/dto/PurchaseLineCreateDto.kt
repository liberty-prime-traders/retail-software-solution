package me.ezra_home.retail_software_solution.locations.business.purchase.dto

import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineCreateDto(
  val locationProductId: UUID,
  val quantityOrdered: BigDecimal,
  val unitCost: BigDecimal
) : Serializable
