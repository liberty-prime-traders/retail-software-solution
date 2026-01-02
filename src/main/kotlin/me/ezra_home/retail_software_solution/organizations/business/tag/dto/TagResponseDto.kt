package me.ezra_home.retail_software_solution.organizations.business.tag.dto

import me.ezra_home.retail_software_solution.util.enums.CategoryType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.TagEntity}
 */
data class TagResponseDto (
    val id: UUID?,
    val category: CategoryType?,
    val tagName: String?,
    val description: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
