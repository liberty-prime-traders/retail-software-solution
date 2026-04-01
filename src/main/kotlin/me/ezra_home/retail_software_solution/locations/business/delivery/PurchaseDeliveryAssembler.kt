package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryResponseDto
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
  private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
  private val unitValueQualifier: UnitValueQualifier
) {

  private data class LineResolutionContext(
    val purchaseLineById: Map<UUID, PurchaseLineEntity>,
    val productMap: Map<UUID?, LocationProductEntity>
  )

  fun buildResponses(
    deliveries: List<PurchaseDeliveryEntity>,
    purchaseLines: List<PurchaseLineEntity>,
    productMap: Map<UUID?, LocationProductEntity>
  ): List<PurchaseDeliveryResponseDto> {
    if (deliveries.isEmpty()) return emptyList()
    val deliveryIds = deliveries.map { it.getNullSafeId() }
    val allDeliveryLines = purchaseDeliveryLineRepository.findByPurchaseDeliveryIdIn(deliveryIds)
    val context = LineResolutionContext(purchaseLines.associateBy { it.getNullSafeId() }, productMap)
    val deliveryLinesByDeliveryId = allDeliveryLines.groupBy { it.purchaseDeliveryId }
    return deliveries.map { delivery ->
      val deliveryLines = deliveryLinesByDeliveryId[delivery.id] ?: emptyList()
      toDto(delivery, deliveryLines, context)
    }
  }

  private fun toDto(
    delivery: PurchaseDeliveryEntity,
    deliveryLines: List<PurchaseDeliveryLineEntity>,
    context: LineResolutionContext
  ): PurchaseDeliveryResponseDto {
    return PurchaseDeliveryResponseDto(
      id = delivery.getNullSafeId(),
      referenceNumber = delivery.getNullSafeReferenceNumber(),
      purchaseId = delivery.purchaseId,
      status = delivery.status,
      deliveredAt = delivery.deliveredAt,
      notes = delivery.notes,
      lines = deliveryLines.map { dl ->
        val purchaseLine = context.purchaseLineById[dl.purchaseLineId]!!
        val product = context.productMap[purchaseLine.locationProductId]!!
        PurchaseDeliveryLineResponseDto(
          id = dl.getNullSafeId(),
          referenceNumber = dl.getNullSafeReferenceNumber(),
          quantityDelivered = dl.quantityDelivered,
          unitCost = dl.unitCost,
          purchaseLineId = dl.purchaseLineId,
          locationProduct = PurchaseLineProductDto(
            referenceNumber = product.getNullSafeReferenceNumber(),
            productName = product.productName,
            productGroupName = product.productGroupName,
            baseUnit = unitValueQualifier.getUnitName(product.baseUnitId)
          )
        )
      }
    )
  }
}
