package me.ezra_home.retail_software_solution.business.category.dto

import me.ezra_home.retail_software_solution.model.enums.CategoryType
import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.CategoryEntity}
 */
data class CategoryInsertDto(
    val categoryType: CategoryType? = null,
    val categoryName: String? = null,
    val description: String? = null,
) : Serializable