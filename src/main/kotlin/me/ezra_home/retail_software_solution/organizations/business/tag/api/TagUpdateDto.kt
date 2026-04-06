package me.ezra_home.retail_software_solution.organizations.business.tag.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class TagUpdateDto(
    val id: UUID? = null,
    val category: Optional<CategoryType>? = null,
    val tagName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable {

    fun applyTo(existing: TagDto): TagDto = existing.copy(
        category = category?.orElse(existing.category) ?: existing.category,
        tagName = tagName?.orElse(existing.tagName) ?: existing.tagName,
        description = description?.orElse(existing.description) ?: existing.description
    )
}
