package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val locationProduct: LocationProductSummaryDto,
  val quantityOrdered: BigDecimal,
  val unitId: UUID,
  val conversionFactor: BigDecimal,
  val unitCost: BigDecimal,
  val lineTotal: BigDecimal,
  val quantityDelivered: BigDecimal,
  val quantityYetToBeDelivered: BigDecimal,
  val quantityCanceled: BigDecimal,
  val quantityExpected: BigDecimal,
) : Serializable
