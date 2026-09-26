package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.locations.business.purchase.HasLocationProduct
import me.ezra_home.retail_software_solution.locations.business.purchase.HasUnitCost
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineCreateDto(
  override val locationProductId: UUID,
  val quantityOrdered: BigDecimal,
  val unitId: UUID,
  override val unitCost: BigDecimal
) : HasLocationProduct, HasUnitCost
