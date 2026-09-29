package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class LocationProductDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val orgProductId: UUID,
    val productName: String,
    val description: String? = null,
    val productGroupName: String,
    val categoryId: UUID,
    val baseUnitId: UUID,
    val defaultSalePrice: BigDecimal? = null,
    val minStockLevel: Int? = null,
    val lastPurchasePrice: BigDecimal? = null,
    val status: ProductStatus,
    val lastSyncedAt: OffsetDateTime? = null
)
