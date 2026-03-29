package me.ezra_home.retail_software_solution.organizations.business.tag.dto

import me.ezra_home.retail_software_solution.organizations.business.tag.CategoryType
import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.TagEntity}
 */
data class TagInsertDto(
    val category: CategoryType,
    val tagName: String,
    val description: String? = null,
) : Serializable
