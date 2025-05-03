package me.ezra_home.retail_software_solution.organizations.business.category.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.CategoryEntity}
 */
data class CategoryUpdateDto (
    val id: UUID? = null,
    val categoryType: Optional<String>? = null,
    val categoryName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable
