package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component

@Component
class PurchaseDeliveryRevisionValidator(
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val deliveryLineStockInspector: DeliveryLineStockInspector
) {

  fun ensureLineIsActive(purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity) {
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

  fun ensureNoTransferredStock(
    deliveryReferenceNumber: String,
    purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity
  ) {
    if (deliveryLineStockInspector.hasTransferredOut(
        deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference())
      )
      throw RtsGenericException(
        "Delivery line ${purchaseDeliveryLineEntity.requiredReference()} cannot be" +
                " revised because some of its stock has been transferred out."
      )
  }

  fun ensureNoMovementsThatCannotBeReversed(deliveryReferenceNumber: String, purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity) {
    if (deliveryLineStockInspector.hasMovementsThatCannotBeReversed(deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference()))
      throw RtsGenericException(
        "Delivery line ${purchaseDeliveryLineEntity.requiredReference()} cannot be revised because its stock has a movement that cannot be reversed."
      )
  }

  fun validate(
    deliveryReferenceNumber: String,
    purchaseLineDto: PurchaseLineDto,
    revisedLine: PurchaseDeliveryLineEntity,
    replacementStorageQuantity: ExactQuantity
  ) {
    val errors = listOf(
      { ensureLineIsActive(revisedLine) },
      { ensurePendingDeliveryQtyIsNotExceeded(purchaseLineDto, revisedLine, replacementStorageQuantity) },
      { ensureNoTransferredStock(deliveryReferenceNumber, revisedLine) },
      { ensureNoMovementsThatCannotBeReversed(deliveryReferenceNumber, revisedLine) }
    ).mapNotNull { check -> errorMessageOf(check) }
    if (errors.isNotEmpty()) throw RtsGenericException(errors.joinToString(" "), payload = errors)
  }

  private fun errorMessageOf(check: () -> Unit): String? =
    try {
      check()
      null
    } catch (rtsGenericException: RtsGenericException) {
      rtsGenericException.message
    }
}
