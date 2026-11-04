package me.ezra_home.retail_software_solution.locations.business.product_purchases_report.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductPurchasesReportParameters(
  val deliveredFrom: OffsetDateTime?,
  val deliveredBefore: OffsetDateTime?,
  val locationProductIds: List<UUID> = emptyList(),
  val categoryIds: List<UUID> = emptyList(),
  val supplierIds: List<UUID> = emptyList()
)
