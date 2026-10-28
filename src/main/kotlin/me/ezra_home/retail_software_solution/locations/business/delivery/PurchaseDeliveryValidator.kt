package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PurchaseDeliveryValidator(
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val stockBreakdownProvider: StockBreakdownProvider
) {

  fun validate(purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto, purchaseLineById: Map<UUID, PurchaseLineDto>) {
    if (purchaseDeliveryCreateDto.lines.isEmpty())
      throw RtsGenericException("Delivery must have at least one line.")

    val lineIds = purchaseDeliveryCreateDto.lines.map { it.purchaseLineId }
    if (lineIds.size != lineIds.toSet().size)
      throw RtsGenericException("Duplicate purchase line IDs in delivery request.")

    purchaseDeliveryCreateDto.lines.forEach { lineDto ->
      if (lineDto.purchaseLineId !in purchaseLineById)
        throw RtsGenericException("Purchase line ${lineDto.purchaseLineId} does not belong to this purchase.")
    }
  }

  fun guardWithinRemaining(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    storageUnitsPerUserProvidedUnitByPurchaseLineId: Map<UUID, Long>
  ) {
    val locationProducts = locationProductDataFetcher.findSummaryByIds(purchaseLineById.values.map { it.locationProductId }.toSet())
    val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(
      purchaseLineById.values.map { it.locationProductId }.toSet()
    )
    purchaseDeliveryCreateDto.lines.forEach { lineDto ->
      val purchaseLine = purchaseLineById.getValue(lineDto.purchaseLineId)
      val delivered = ExactQuantity.ofStorageFromUserProvided(
        lineDto.userProvidedQuantityDelivered,
        storageUnitsPerUserProvidedUnitByPurchaseLineId.getValue(lineDto.purchaseLineId)
      )
      if (delivered > purchaseLine.storageQuantityRemaining) {
        val quantityBreakdown = breakdownByLocationProductId.getValue(purchaseLine.locationProductId)
        throw RtsGenericException(
          "Maximum quantity that can be delivered for " +
                  "'${locationProducts.getValue(purchaseLine.locationProductId).label}' is " +
                  "${quantityBreakdown.render(purchaseLine.storageQuantityRemaining)}." +
                  " However, ${quantityBreakdown.render(delivered)} was provided."
        )
      }
    }
  }
}
