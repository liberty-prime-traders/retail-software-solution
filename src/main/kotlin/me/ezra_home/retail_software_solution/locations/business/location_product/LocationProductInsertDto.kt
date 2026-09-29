package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class LocationProductInsertDto(
    val orgProductId: UUID,
    val productName: String,
    val description: String? = null,
    val productGroupName: String,
    val categoryId: UUID,
    val baseUnitId: UUID,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val lastSyncedAt: OffsetDateTime? = null
) : Serializable
