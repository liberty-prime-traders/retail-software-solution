package me.ezra_home.retail_software_solution.organizations.business.product_group.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class ProductGroupUpdateDto(
    val id: UUID,
    val groupName: Optional<String>? = null,
    val description: Optional<String>? = null,
    val categoryId: Optional<UUID>? = null
) : Serializable {

    fun applyTo(existing: ProductGroupDto): ProductGroupDto = existing.copy(
        groupName = groupName?.orElse(existing.groupName) ?: existing.groupName,
        description = description?.orElse(existing.description) ?: existing.description,
        categoryId = categoryId?.orElse(existing.categoryId) ?: existing.categoryId
    )
}
