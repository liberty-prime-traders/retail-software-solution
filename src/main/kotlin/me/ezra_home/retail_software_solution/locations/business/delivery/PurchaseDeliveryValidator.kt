package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryCreateDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PurchaseDeliveryValidator(
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val productStockBreakdownProvider: ProductStockBreakdownProvider
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

  fun ensurePendingDeliveryQuantityIsNotExceeded(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    storageUnitsPerUserProvidedUnitByPurchaseLineId: Map<UUID, Long>
  ) {
    val locationProducts = locationProductDataFetcher.findSummaryByIds(
      purchaseLineById.values.map { it.locationProductId }.toSet()
    )
    val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
      purchaseLineById.values.map { it.locationProductId }.toSet()
    )
    purchaseDeliveryCreateDto.lines.forEach { lineDto ->
      val purchaseLine = purchaseLineById.getValue(lineDto.purchaseLineId)
      val delivered = ExactQuantity.ofStorageFromUserProvided(
        lineDto.userProvidedQuantityDelivered,
        storageUnitsPerUserProvidedUnitByPurchaseLineId.getValue(lineDto.purchaseLineId)
      )
      if (delivered > purchaseLine.storageQuantityRemaining) {
        throw RtsGenericException(
          maximumQuantityMessageOf(
            locationProducts.getValue(purchaseLine.locationProductId).label,
            breakdownByLocationProductId.getValue(purchaseLine.locationProductId),
            purchaseLine.storageQuantityRemaining,
            delivered
          )
        )
      }
    }
  }

  fun maximumQuantityProblem(
    documentReference: String,
    locationProductId: UUID,
    maximumQuantity: ExactQuantity,
    providedQuantity: ExactQuantity
  ): BlockingProblem? {
    if (providedQuantity <= maximumQuantity) return null
    val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(locationProductId)
    return BlockingProblem(
      documentReference = documentReference,
      description = maximumQuantityMessageOf(
        locationProductDataFetcher.findSummaryByIds(setOf(locationProductId)).getValue(locationProductId).label,
        quantityBreakdown,
        maximumQuantity,
        providedQuantity
      ),
      effect = "Enter ${quantityBreakdown.render(maximumQuantity)} or less."
    )
  }

  private fun maximumQuantityMessageOf(
    locationProductLabel: String,
    quantityBreakdown: QuantityBreakdown,
    maximumQuantity: ExactQuantity,
    providedQuantity: ExactQuantity
  ): String {
    val maximumQuantityText = quantityBreakdown.render(maximumQuantity)
    val providedQuantityText = quantityBreakdown.render(providedQuantity)
    return "You can deliver at most $maximumQuantityText of '$locationProductLabel', " +
      "but you entered $providedQuantityText."
  }
}
