package me.ezra_home.retail_software_solution.organizations.business.product_category.dto

import java.time.OffsetDateTime
import java.util.UUID

data class ProductCategoryDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var categoryName: String? = null,
    var description: String? = null
)
