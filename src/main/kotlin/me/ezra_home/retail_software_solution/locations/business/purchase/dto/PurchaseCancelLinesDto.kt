package me.ezra_home.retail_software_solution.locations.business.purchase.dto

import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineCancelDto(
  val locationProductId: UUID,
  val quantityCanceled: BigDecimal
) : Serializable
