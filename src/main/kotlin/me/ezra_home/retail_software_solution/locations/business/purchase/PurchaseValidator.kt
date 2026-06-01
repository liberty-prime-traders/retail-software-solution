package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal

object PurchaseValidator {

  fun guardNoDuplicateProducts(lines: List<HasLocationProduct>) {
    val productIds = lines.map { it.locationProductId }
    if (productIds.size != productIds.toSet().size)
      throw RtsGenericException("A purchase cannot have more than one line for the same product.")
  }

  fun guardHasLines(lines: List<*>) {
    if (lines.isEmpty())
      throw RtsGenericException("An order must have at least one item.")
  }

  fun guardPositiveLineQuantities(lines: List<PurchaseLineCreateDto>) {
    if (lines.any { it.quantityOrdered.signum() <= 0 })
      throw RtsGenericException("Line quantity must be positive.")
  }

  fun guardNonNegativeUpdateQuantity(quantityOrdered: BigDecimal) {
    if (quantityOrdered.signum() < 0)
      throw RtsGenericException("Line quantity cannot be negative; send 0 to delete a line.")
  }

  fun guardIsDraft(purchase: PurchaseEntity) {
    if (purchase.purchaseStatus != PurchaseStatus.DRAFT)
      throw RtsGenericException("Purchase is not in draft status anymore. The requested updates cannot be applied.")
  }

  fun guardCanCancelLines(purchase: PurchaseEntity) {
    if (purchase.purchaseStatus !in listOf(PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED))
      throw RtsGenericException("Cannot cancel lines on a purchase with status ${purchase.purchaseStatus}")
  }

  fun guardCanReceiveDelivery(purchase: PurchaseEntity) {
    when (purchase.purchaseStatus) {
      PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED -> return
      PurchaseStatus.DRAFT ->
        throw RtsGenericException("Cannot record a delivery on a draft purchase. Confirm the order first.")
      PurchaseStatus.FULLY_DELIVERED ->
        throw RtsGenericException("Cannot record a delivery on a fully delivered purchase.")
      PurchaseStatus.CANCELED ->
        throw RtsGenericException("Cannot record a delivery on a canceled purchase.")
    }
  }

  fun guardCancelQuantity(line: PurchaseLineEntity, cancelQty: BigDecimal) {
    val maxCancelable = line.quantityOrdered - line.quantityDelivered
    if (cancelQty > maxCancelable)
      throw RtsGenericException(
        "Cannot cancel $cancelQty items of line ${line.id}. " +
          "Ordered: ${line.quantityOrdered}, Delivered: ${line.quantityDelivered}, Max cancelable: $maxCancelable"
      )
  }
}
