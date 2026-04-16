package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseCancelLinesDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
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
    if (purchase.purchaseStatus != PurchaseStatus.DRAFT)
      throw RtsGenericException("Purchase is not in draft status anymore. The requested updates cannot be applied.")
  }

  fun guardCanCancelLines(purchase: PurchaseEntity) {
    if (purchase.purchaseStatus !in listOf(PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED))
      throw RtsGenericException("Cannot cancel lines on a purchase with status ${purchase.purchaseStatus}")
  }

  fun guardNoInactiveProducts(products: List<LocationProductEntity>) {
    val inactive = products.filter { it.status != ProductStatus.ACTIVE }
    if (inactive.isNotEmpty()) {
      val names = inactive.joinToString { it.productName }
      throw RtsGenericException("The following products are not active and cannot be added to a purchase: $names")
    }
  }

  fun guardCancelQuantity(line: PurchaseLineEntity, cancel: PurchaseCancelLinesDto) {
    val maxCancelable = line.quantityOrdered - line.quantityDelivered
    if (cancel.quantityCanceled > maxCancelable)
      throw RtsGenericException(
        "Cannot cancel ${cancel.quantityCanceled} items of line ${cancel.purchaseLineId}. " +
          "Ordered: ${line.quantityOrdered}, Delivered: ${line.quantityDelivered}, Max cancelable: $maxCancelable"
      )
  }
}
