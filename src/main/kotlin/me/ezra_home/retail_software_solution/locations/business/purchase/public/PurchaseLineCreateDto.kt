package me.ezra_home.retail_software_solution.locations.business.purchase.public

import me.ezra_home.retail_software_solution.locations.business.purchase.HasLocationProduct
import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineCreateDto(
  override val locationProductId: UUID,
  val quantityOrdered: BigDecimal,
  val unitCost: BigDecimal
) : HasLocationProduct, Serializable
