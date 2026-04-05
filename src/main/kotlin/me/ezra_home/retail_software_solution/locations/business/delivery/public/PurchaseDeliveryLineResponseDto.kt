package me.ezra_home.retail_software_solution.locations.business.delivery.public

import me.ezra_home.retail_software_solution.locations.business.purchase.public.PurchaseLineProductDto
import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveryLineResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseLineId: UUID,
  val locationProduct: PurchaseLineProductDto,
  val quantityDelivered: BigDecimal,
  val unitCost: BigDecimal
)
