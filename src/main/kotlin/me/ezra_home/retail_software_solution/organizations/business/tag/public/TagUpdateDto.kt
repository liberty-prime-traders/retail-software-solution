package me.ezra_home.retail_software_solution.organizations.business.tag.public

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class TagUpdateDto(
    val id: UUID? = null,
    val category: Optional<CategoryType>? = null,
    val tagName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable
