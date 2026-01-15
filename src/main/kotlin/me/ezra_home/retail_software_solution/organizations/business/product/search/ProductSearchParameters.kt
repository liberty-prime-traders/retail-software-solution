package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import java.util.UUID

data class ProductSearchParameters(
    val productName: String? = null,
    val referenceNumber: String? = null,
    val description: String? = null,
    val categoryIds: Set<UUID> = emptySet(),
    val tagsIds: Set<UUID> = emptySet(),
    val statusList: Set<ProductStatus> = setOf(ProductStatus.ACTIVE)
)
