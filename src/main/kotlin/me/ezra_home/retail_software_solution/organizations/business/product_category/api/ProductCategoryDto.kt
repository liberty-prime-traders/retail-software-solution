package me.ezra_home.retail_software_solution.organizations.business.product_category.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductCategoryDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val categoryName: String? = null,
    val description: String? = null
)
