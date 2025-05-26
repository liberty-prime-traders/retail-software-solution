package me.ezra_home.retail_software_solution.organizations.business.category.dto

import me.ezra_home.retail_software_solution.util.enums.CategoryType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.CategoryEntity}
 */
data class CategoryResponseDto (
    val id: UUID?,
    val categoryType: CategoryType?,
    val categoryName: String?,
    val description: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?
) : Serializable
