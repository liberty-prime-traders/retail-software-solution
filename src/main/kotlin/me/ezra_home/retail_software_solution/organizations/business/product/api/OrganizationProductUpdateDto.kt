package me.ezra_home.retail_software_solution.organizations.business.product.api

import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductDto
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class OrganizationProductUpdateDto(
    val id: UUID,
    val productName: Optional<String>? = null,
    val description: Optional<String>? = null,
    val productGroupId: Optional<UUID>? = null,
    val tagsToAdd: Set<UUID> = emptySet(),
    val tagsToRemove: Set<UUID> = emptySet()
) : Serializable {

    fun applyTo(existing: OrganizationProductDto): OrganizationProductDto = existing.copy(
        productName = productName?.orElse(existing.productName) ?: existing.productName,
        description = description?.orElse(existing.description) ?: existing.description,
        productGroupId = productGroupId?.orElse(existing.productGroupId) ?: existing.productGroupId
    )
}
