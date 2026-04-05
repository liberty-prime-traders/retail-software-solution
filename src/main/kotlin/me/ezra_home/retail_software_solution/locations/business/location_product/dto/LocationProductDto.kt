package me.ezra_home.retail_software_solution.locations.business.location_product.dto

import me.ezra_home.retail_software_solution.organizations.business.product.public.ProductStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class LocationProductDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var productId: UUID? = null,
    var productName: String? = null,
    var description: String? = null,
    var productGroupName: String? = null,
    var categoryId: UUID? = null,
    var baseUnitId: UUID? = null,
    var defaultSalePrice: BigDecimal? = null,
    var minStockLevel: Int? = null,
    var lastPurchasePrice: BigDecimal? = null,
    var status: ProductStatus = ProductStatus.ACTIVE,
    var lastSyncedAt: OffsetDateTime? = null,
    var stockBalance: BigDecimal? = null
)
