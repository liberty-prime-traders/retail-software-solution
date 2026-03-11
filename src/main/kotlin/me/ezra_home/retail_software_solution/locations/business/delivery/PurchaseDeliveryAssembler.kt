package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineProductDto
import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueQualifier
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PurchaseDeliveryAssembler(
  private val locationProductRepository: LocationProductRepository,
  private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
  private val unitValueQualifier: UnitValueQualifier
) {

  fun buildResponses(
    deliveries: List<PurchaseDeliveryEntity>,
    purchaseLines: List<PurchaseLineEntity>
  ): List<PurchaseDeliveryResponseDto> {
    val deliveryIds = deliveries.map { it.id!! }
    val allDeliveryLines = purchaseDeliveryLineRepository.findByPurchaseDeliveryIdIn(deliveryIds)
    val purchaseLineById = purchaseLines.associateBy { it.id!! }
    val deliveryLinesByDeliveryId = allDeliveryLines.groupBy { it.purchaseDeliveryId }
    val locationProductIds = purchaseLines.map { it.locationProductId }
    val productMap = locationProductRepository.findAllById(locationProductIds).associateBy { it.id }

    return deliveries.map { delivery ->
      val deliveryLines = deliveryLinesByDeliveryId[delivery.id] ?: emptyList()
      toDto(delivery, deliveryLines, purchaseLineById, productMap)
    }
  }

  private fun toDto(
    delivery: PurchaseDeliveryEntity,
    deliveryLines: List<PurchaseDeliveryLineEntity>,
    purchaseLineById: Map<UUID, PurchaseLineEntity>,
    productMap: Map<UUID?, LocationProductEntity>
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
        val product = productMap[purchaseLine.locationProductId]!!
        PurchaseDeliveryLineResponseDto(
          id = dl.id!!,
          referenceNumber = dl.referenceNumber!!,
          quantityDelivered = dl.quantityDelivered,
          unitCost = dl.unitCost,
          purchaseLineId = dl.purchaseLineId,
          locationProduct = PurchaseLineProductDto(
            referenceNumber = product.referenceNumber,
            productName = product.productName,
            productGroupName = product.productGroupName,
            baseUnit = unitValueQualifier.getUnitName(product.baseUnitId)
          )
        )
      }
    )
  }
}
