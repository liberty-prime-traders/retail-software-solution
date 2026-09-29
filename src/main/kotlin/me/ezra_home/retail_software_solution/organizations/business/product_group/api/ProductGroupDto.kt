package me.ezra_home.retail_software_solution.organizations.business.product_group.api

import java.time.OffsetDateTime
import java.util.UUID

data class ProductGroupDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val groupName: String,
    val description: String? = null,
    val categoryId: UUID
)
