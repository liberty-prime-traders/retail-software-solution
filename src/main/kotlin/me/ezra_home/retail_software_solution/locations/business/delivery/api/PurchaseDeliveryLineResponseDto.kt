package me.ezra_home.retail_software_solution.locations.business.delivery.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.StripZeroesAndRound
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveryLineResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseLineId: UUID,
  val locationProduct: LocationProductSummaryDto,
  @get:JsonProperty("quantityDelivered")
  val userProvidedQuantityDelivered: BigDecimal,
  val unitId: UUID,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val unitCost: BigDecimal,
) {
  val lineTotal: BigDecimal = Decimals.MoneyScale.multiply(userProvidedQuantityDelivered, unitCost)
}
