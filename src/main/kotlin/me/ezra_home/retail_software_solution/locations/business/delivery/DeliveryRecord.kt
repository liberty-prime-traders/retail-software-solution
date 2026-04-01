package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryEntity
import me.ezra_home.retail_software_solution.locations.model.PurchaseDeliveryLineEntity

data class DeliveryRecord(
  val delivery: PurchaseDeliveryEntity,
  val lines: List<PurchaseDeliveryLineEntity>
)
