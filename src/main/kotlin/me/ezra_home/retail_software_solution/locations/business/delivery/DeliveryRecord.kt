package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity

data class DeliveryRecord(
  val delivery: PurchaseDeliveryEntity,
  val lines: List<PurchaseDeliveryLineEntity>
)
