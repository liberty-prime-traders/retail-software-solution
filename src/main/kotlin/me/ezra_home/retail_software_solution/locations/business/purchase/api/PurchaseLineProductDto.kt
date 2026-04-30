package me.ezra_home.retail_software_solution.locations.business.purchase.api

import java.util.UUID

data class PurchaseLineProductDto(
  val productName: String,
  val referenceNumber: String,
  val productGroupName: String?,
  val baseUnitId: UUID,
)
