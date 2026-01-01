package me.ezra_home.retail_software_solution.organizations.business.tag.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.TagEntity}
 */
data class TagInsertDto(
    val tagName: String? = null,
    val description: String? = null,
) : Serializable
