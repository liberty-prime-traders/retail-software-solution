package me.ezra_home.retail_software_solution.organizations.business.tag.dto

import me.ezra_home.retail_software_solution.util.enums.CategoryType
import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.TagEntity}
 */
data class TagUpdateDto (
    val id: UUID? = null,
    val category: Optional<CategoryType>? = null,
    val tagName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable
