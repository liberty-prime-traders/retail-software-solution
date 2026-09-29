package me.ezra_home.retail_software_solution.organizations.business.tag.api

import java.time.OffsetDateTime
import java.util.UUID

data class TagDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val category: CategoryType,
    val tagName: String,
    val description: String? = null
)
