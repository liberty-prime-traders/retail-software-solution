package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import java.util.UUID

data class LocationProductSyncDto(
    val productId: UUID,
    val productName: String,
    val description: String?,
    val productGroupName: String?,
    val status: ProductStatus,
    val baseUnitId: UUID,
    val categoryId: UUID
)
