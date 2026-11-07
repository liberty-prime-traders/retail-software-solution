package me.ezra_home.retail_software_solution.locations.business.delivery

import java.util.UUID

interface PurchaseDeliveryLineOwners {
  val purchaseId: UUID
  val purchaseLineId: UUID
}
