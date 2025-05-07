package me.ezra_home.retail_software_solution.organizations.business.category.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.CategoryEntity}
 */
data class CategoryInsertDto(
    val categoryType: String? = null,
    val categoryName: String? = null,
    val description: String? = null,
) : Serializable
