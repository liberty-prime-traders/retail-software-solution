package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineProductDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PurchaseDeliveryAssembler(
  private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
) {

  fun buildResponses(
    deliveries: List<PurchaseDeliveryEntity>,
    purchaseLines: Map<UUID, PurchaseLineDto>,
    productSummaries: Map<UUID, LocationProductSummaryDto>
  ): List<PurchaseDeliveryResponseDto> {
    if (deliveries.isEmpty()) return emptyList()
    val deliveryIds = deliveries.map { it.id!! }
    val allDeliveryLines = purchaseDeliveryLineRepository.findByPurchaseDeliveryIdIn(deliveryIds)
    val deliveryLinesByDeliveryId = allDeliveryLines.groupBy { it.purchaseDeliveryId }
    return deliveries.map { delivery ->
      val deliveryLines = deliveryLinesByDeliveryId[delivery.id] ?: emptyList()
      toDto(delivery, deliveryLines, purchaseLines, productSummaries)
    }
  }

  private fun toDto(
    delivery: PurchaseDeliveryEntity,
    deliveryLines: List<PurchaseDeliveryLineEntity>,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    productSummaries: Map<UUID, LocationProductSummaryDto>
  ): PurchaseDeliveryResponseDto {
    return PurchaseDeliveryResponseDto(
      id = delivery.id!!,
      referenceNumber = delivery.referenceNumber!!,
      purchaseId = delivery.purchaseId,
      status = delivery.status,
      deliveredAt = delivery.deliveredAt,
      notes = delivery.notes,
      lines = deliveryLines.map { dl ->
        val purchaseLine = purchaseLineById[dl.purchaseLineId]!!
        val product = productSummaries[purchaseLine.locationProductId]!!
        PurchaseDeliveryLineResponseDto(
          id = dl.id!!,
          referenceNumber = dl.referenceNumber!!,
          quantityDelivered = dl.quantityDelivered,
          unitId = dl.unitId,
          unitCost = dl.unitCost,
          purchaseLineId = dl.purchaseLineId,
          locationProduct = PurchaseLineProductDto(
            referenceNumber = product.referenceNumber,
            productName = product.productName,
            productGroupName = product.productGroupName,
            baseUnitId = product.baseUnitId
          )
        )
      }
    )
  }
}
