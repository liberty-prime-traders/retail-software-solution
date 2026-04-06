package me.ezra_home.retail_software_solution.organizations.business.product_group.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductGroupDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var groupName: String,
    var description: String? = null,
    var categoryId: UUID
)
