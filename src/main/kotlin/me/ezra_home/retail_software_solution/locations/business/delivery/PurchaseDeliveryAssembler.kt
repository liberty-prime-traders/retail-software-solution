package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryLineResponseDto
import me.ezra_home.retail_software_solution.locations.business.delivery.dto.PurchaseDeliveryResponseDto
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineProductDto
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueQualifier
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PurchaseDeliveryAssembler(
  private val locationProductRepository: LocationProductRepository,
  private val unitValueQualifier: UnitValueQualifier
) {

  fun buildResponse(
    delivery: PurchaseDeliveryEntity,
    deliveryLines: List<PurchaseDeliveryLineEntity>,
    purchaseLineById: Map<UUID, PurchaseLineEntity>
  ): PurchaseDeliveryResponseDto {
    val locationProductIds = purchaseLineById.values.map { it.locationProductId }
    val productMap = locationProductRepository.findAllById(locationProductIds).associateBy { it.id }

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
          locationProductId = purchaseLine.locationProductId,
          locationProduct = PurchaseLineProductDto(
            locationProductId = product.id!!,
            productName = product.productName,
            description = product.description,
            productGroupName = product.productGroupName,
            baseUnit = unitValueQualifier.getUnitName(product.baseUnitId)
          ),
          quantityDelivered = dl.quantityDelivered,
          unitCost = dl.unitCost
        )
      }
    )
  }
}
