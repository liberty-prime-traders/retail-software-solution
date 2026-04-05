package me.ezra_home.retail_software_solution.locations.business.purchase.public

import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class PurchaseCancelLinesDto(
  val purchaseLineId: UUID,
  val quantityCanceled: BigDecimal
) : Serializable
