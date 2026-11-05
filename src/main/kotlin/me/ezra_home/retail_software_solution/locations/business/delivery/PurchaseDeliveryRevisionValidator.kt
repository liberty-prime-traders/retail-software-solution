package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.DeliveryLineTransferInspector
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component

@Component
class PurchaseDeliveryRevisionValidator(
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val deliveryLineTransferInspector: DeliveryLineTransferInspector
) {

  fun guardLineIsActive(purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity) {
    if (purchaseDeliveryLineEntity.status != PurchaseDeliveryLineStatus.ACTIVE)
      throw RtsGenericException(
        "Delivery line ${purchaseDeliveryLineEntity.requiredReference()} has already been revised. Revise its current line instead."
      )
  }

  fun ensurePendingDeliveryQtyIsNotExceeded(
    purchaseLineDto: PurchaseLineDto,
    revisedLine: PurchaseDeliveryLineEntity,
    replacementStorageQuantity: ExactQuantity
  ) {
    val revisedStorageQuantity = ExactQuantity.ofStorageFromUserProvided(
      revisedLine.userProvidedQuantityDelivered,
      revisedLine.storageUnitsPerUserProvidedUnit
    )
    val netRemaining = purchaseLineDto.storageQuantityRemaining + revisedStorageQuantity
    purchaseDeliveryValidator.ensureWithinMaximumQuantity(purchaseLineDto, netRemaining, replacementStorageQuantity)
  }

  fun guardNoTransferredStock(
    deliveryReferenceNumber: String,
    purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity
  ) {
    if (deliveryLineTransferInspector.hasTransferredOut(
        deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference())
      )
      throw RtsGenericException(
        "Delivery line ${purchaseDeliveryLineEntity.requiredReference()} cannot be" +
                " revised because some of its stock has been transferred out."
      )
  }
}
