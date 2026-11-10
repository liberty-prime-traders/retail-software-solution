package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component

@Component
class PurchaseDeliveryRevisionValidator(
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val deliveryLineStockInspector: DeliveryLineStockInspector
) {

  fun problemIfLineIsNotActive(purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity): BlockingProblem? {
    if (purchaseDeliveryLineEntity.status == PurchaseDeliveryLineStatus.ACTIVE) return null
    return BlockingProblem(
      documentReference = purchaseDeliveryLineEntity.requiredReference(),
      description = "This delivery line has already been changed.",
      effect = "Change its latest line instead."
    )
  }

  fun problemIfPendingDeliveryQuantityIsExceeded(
    purchaseLineDto: PurchaseLineDto,
    revisedLine: PurchaseDeliveryLineEntity,
    replacementStorageQuantity: ExactQuantity
  ): BlockingProblem? {
    val revisedStorageQuantity = ExactQuantity.ofStorageFromUserProvided(
      revisedLine.userProvidedQuantityDelivered,
      revisedLine.storageUnitsPerUserProvidedUnit
    )
    val netRemaining = purchaseLineDto.storageQuantityRemaining + revisedStorageQuantity
    return purchaseDeliveryValidator.maximumQuantityProblem(
      revisedLine.requiredReference(), purchaseLineDto.locationProductId, netRemaining, replacementStorageQuantity
    )
  }

  fun problemIfStockWasTransferred(
    deliveryReferenceNumber: String,
    purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity
  ): BlockingProblem? {
    if (!deliveryLineStockInspector.hasTransferredOut(
        deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference())
    ) return null
    return BlockingProblem(
      documentReference = purchaseDeliveryLineEntity.requiredReference(),
      description = "Some of this delivery's stock has already been sent to another branch.",
      effect = "This line cannot be changed."
    )
  }

  fun problemIfStockHasMovementsThatCannotBeReversed(
    deliveryReferenceNumber: String,
    purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity
  ): BlockingProblem? {
    val hasIrreversibleMovements = deliveryLineStockInspector.hasMovementsThatCannotBeReversed(
      deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference()
    )
    if (hasIrreversibleMovements) {
      return BlockingProblem(
        documentReference = purchaseDeliveryLineEntity.requiredReference(),
        description = "Some of this delivery's stock was returned, and returns cannot be undone.",
        effect = "This line cannot be changed."
      )
    }
    return null
  }

  fun problemsOf(
    deliveryReferenceNumber: String,
    purchaseLineDto: PurchaseLineDto,
    revisedLine: PurchaseDeliveryLineEntity,
    replacementStorageQuantity: ExactQuantity
  ): List<BlockingProblem> = listOfNotNull(
    problemIfLineIsNotActive(revisedLine),
    problemIfPendingDeliveryQuantityIsExceeded(purchaseLineDto, revisedLine, replacementStorageQuantity),
    problemIfStockWasTransferred(deliveryReferenceNumber, revisedLine),
    problemIfStockHasMovementsThatCannotBeReversed(deliveryReferenceNumber, revisedLine)
  )
}
