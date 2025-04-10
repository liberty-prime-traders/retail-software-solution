package me.ezra_home.retail_software_solution.locations.business.category.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.CategoryEntity}
 */
data class CategoryResponseDto (
    val id: UUID?,
    val categoryType: String?,
    val categoryName: String?,
    val description: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?
) : Serializable
