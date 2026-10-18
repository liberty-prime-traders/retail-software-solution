package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
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
    if (lines.any { it.userProvidedQuantityOrdered.signum() <= 0 })
      throw RtsGenericException("Line quantity must be positive.")
  }

  fun guardPositiveUnitCosts(lines: List<HasUnitCost>) {
    if (lines.any { it.unitCost.signum() <= 0 })
      throw RtsGenericException("Line unit cost must be positive.")
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

  // Cancel is absolute: it replaces the counter, which plus delivered may not exceed ordered, in storage units
  fun guardCancelQuantity(line: PurchaseLineEntity, storageQuantityToCancel: ExactQuantity, quantityBreakdown: QuantityBreakdown) {
    val maxCancelable = line.storageQuantityOrdered() - ExactQuantity.ofStorage(line.storageQuantityDelivered)
    if (storageQuantityToCancel > maxCancelable)
      throw RtsGenericException(
        "Cannot cancel ${quantityBreakdown.render(storageQuantityToCancel)} of line ${line.requiredReference()}. " +
          "Ordered: ${quantityBreakdown.render(line.storageQuantityOrdered())}, " +
          "Delivered: ${quantityBreakdown.render(ExactQuantity.ofStorage(line.storageQuantityDelivered))}, " +
          "Max cancelable: ${quantityBreakdown.render(maxCancelable)}"
      )
  }
}
