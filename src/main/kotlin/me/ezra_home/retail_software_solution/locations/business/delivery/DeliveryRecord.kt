package me.ezra_home.retail_software_solution.locations.business.delivery

data class DeliveryRecord(
  val delivery: PurchaseDeliveryEntity,
  val lines: List<PurchaseDeliveryLineEntity>
)
