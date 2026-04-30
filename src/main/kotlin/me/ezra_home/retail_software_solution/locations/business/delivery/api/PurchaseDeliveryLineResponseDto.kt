package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineProductDto
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveryLineResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseLineId: UUID,
  val locationProduct: PurchaseLineProductDto,
  val quantityDelivered: BigDecimal,
  val unitId: UUID,
  val unitCost: BigDecimal,
) {
  val lineTotal: BigDecimal get() = Decimals.multiplyScale4(quantityDelivered, unitCost)
}
