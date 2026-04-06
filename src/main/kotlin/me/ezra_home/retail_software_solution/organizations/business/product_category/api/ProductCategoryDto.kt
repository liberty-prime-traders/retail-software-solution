package me.ezra_home.retail_software_solution.organizations.business.product_category.api

import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class ProductCategoryDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var categoryName: String? = null,
    var description: String? = null
): HasId
