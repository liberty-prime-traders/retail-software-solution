package me.ezra_home.retail_software_solution.locations.business.sync.sync_services.product

import me.ezra_home.retail_software_solution.locations.model.LocationProductEntity
import java.time.OffsetDateTime

object LocationProductMapper {

  fun toLocationProduct(syncData: ProductSyncData): LocationProductEntity {
    return LocationProductEntity(
      productId = syncData.productId,
      productName = syncData.productName,
      description = syncData.description,
      productGroupName = syncData.productGroupName ?: "Unknown",
      categoryId = syncData.categoryId,
      baseUnitId = syncData.baseUnitId,
      status = syncData.status,
      lastSyncedAt = OffsetDateTime.now()
    )
  }
}
