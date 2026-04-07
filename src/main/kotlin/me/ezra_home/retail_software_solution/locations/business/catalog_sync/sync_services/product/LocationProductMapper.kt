package me.ezra_home.retail_software_solution.locations.business.catalog_sync.sync_services.product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSyncDto

object LocationProductMapper {

  fun toSyncDto(syncData: ProductSyncData): LocationProductSyncDto {
    return LocationProductSyncDto(
      productId = syncData.productId,
      productName = syncData.productName,
      description = syncData.description,
      productGroupName = syncData.productGroupName,
      status = syncData.status,
      categoryId = syncData.categoryId,
      baseUnitId = syncData.baseUnitId
    )
  }
}
