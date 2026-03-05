package me.ezra_home.retail_software_solution.locations.business.purchase.dto

import java.util.UUID

data class PurchaseLineProductDto(
  val locationProductId: UUID,
  val productName: String?,
  val description: String?,
  val productGroupName: String?,
  val baseUnit: String?
)
