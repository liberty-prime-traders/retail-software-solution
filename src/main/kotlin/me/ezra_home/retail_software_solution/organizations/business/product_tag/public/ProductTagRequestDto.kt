package me.ezra_home.retail_software_solution.organizations.business.product_tag.public

import java.io.Serializable
import java.util.UUID

data class ProductTagRequestDto(
    val tagsToAdd: Set<UUID> = emptySet(),
    val tagsToRemove: Set<UUID> = emptySet()
) : Serializable
