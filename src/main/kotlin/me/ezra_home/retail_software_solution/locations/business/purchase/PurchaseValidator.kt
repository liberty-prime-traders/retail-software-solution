package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineCancelDto
import me.ezra_home.retail_software_solution.locations.business.purchase.dto.PurchaseLineUpdateDto
import me.ezra_home.retail_software_solution.locations.model.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseLineEntity
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

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

  fun guardIsDraft(purchase: PurchaseEntity) {
    if (purchase.status != PurchaseStatus.DRAFT)
      throw RtsGenericException("Purchase is not in draft status anymore. The requested updates cannot be applied.")
  }

  fun guardCanCancelLines(purchase: PurchaseEntity) {
    if (purchase.status !in listOf(PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED))
      throw RtsGenericException("Cannot cancel lines on a purchase with status ${purchase.status}")
  }

  fun guardNewLineHasProduct(dto: PurchaseLineUpdateDto) {
    if (dto.locationProductId == null)
      throw RtsGenericException("locationProductId is required for new purchase lines.")
  }

  fun guardCancelQuantity(line: PurchaseLineEntity, cancel: PurchaseLineCancelDto) {
    val maxCancelable = line.quantityOrdered - line.quantityDelivered
    if (cancel.quantityCanceled > maxCancelable)
      throw RtsGenericException(
        "Cannot cancel ${cancel.quantityCanceled} items of line ${cancel.purchaseLineId}. " +
          "Ordered: ${line.quantityOrdered}, Delivered: ${line.quantityDelivered}, Max cancelable: $maxCancelable"
      )
  }
}
