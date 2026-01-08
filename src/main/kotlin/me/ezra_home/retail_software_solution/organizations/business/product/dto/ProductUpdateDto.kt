package me.ezra_home.retail_software_solution.organizations.business.product.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductEntity}
 */
data class ProductUpdateDto (
    val id: UUID,
    val productName: Optional<String>? = null,
    val description: Optional<String>? = null,
    val categoryId: Optional<UUID>? = null,
    val baseUnitId: Optional<UUID>? = null,
    val tagsToAdd: Set<UUID> = emptySet(),
    val tagsToRemove: Set<UUID> = emptySet()
) : Serializable
