package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphBuilder
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class PurchaseDeliveryValidator(
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val unitConversionGraphBuilder: UnitConversionGraphBuilder
) {

  fun validate(dto: PurchaseDeliveryCreateDto, purchaseLineById: Map<UUID, PurchaseLineDto>) {
    if (dto.lines.isEmpty())
      throw RtsGenericException("Delivery must have at least one line.")

    val lineIds = dto.lines.map { it.purchaseLineId }
    if (lineIds.size != lineIds.toSet().size)
      throw RtsGenericException("Duplicate purchase line IDs in delivery request.")

    val locationProducts = locationProductDataFetcher.findSummaryByIds(
      purchaseLineById.values.map { it.locationProductId }.toSet()
    )

    dto.lines.forEach { lineDto ->
      val purchaseLine = purchaseLineById[lineDto.purchaseLineId]
        ?: throw RtsGenericException("Purchase line ${lineDto.purchaseLineId} does not belong to this purchase.")

      val orderedProduct = locationProducts[purchaseLine.locationProductId]!!

      if (lineDto.quantityDelivered <= BigDecimal.ZERO)
        throw RtsGenericException("Quantity delivered must be greater than zero for '${orderedProduct.label}'.")

      if (lineDto.unitCost <= BigDecimal.ZERO)
        throw RtsGenericException("Unit cost must be greater than zero for '${orderedProduct.label}'.")

      val factor = unitConversionGraphBuilder.getFactor(lineDto.unitId, purchaseLine.unitId)
      val deliveredInPurchaseLineUnit = Decimals.multiplyScale4(lineDto.quantityDelivered, factor)
      val remaining = purchaseLine.remainingQuantity
      if (deliveredInPurchaseLineUnit > remaining)
        throw RtsGenericException(
          "Maximum quantity that can be delivered for '${orderedProduct.label}' is $remaining." +
                  " However, $deliveredInPurchaseLineUnit was provided."
        )
    }
  }
}
