package me.ezra_home.retail_software_solution.organizations.business.tag.public

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class TagResponseDto(
    val id: UUID,
    val category: CategoryType?,
    val tagName: String?,
    val description: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
