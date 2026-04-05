package me.ezra_home.retail_software_solution.organizations.business.product_group.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class ProductGroupUpdateDto(
    val id: UUID,
    val groupName: Optional<String>? = null,
    val description: Optional<String>? = null,
    val categoryId: Optional<UUID>? = null
) : Serializable
