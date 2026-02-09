package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.product

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import java.util.UUID

data class ProductSyncData(
  val productId: UUID,
  val productName: String,
  val description: String?,
  val productGroupName: String?,
  val status: ProductStatus,
  val referenceNumber: String?,
  val baseUnitId: UUID,
  val categoryId: UUID,
  val revision: Long?
)
