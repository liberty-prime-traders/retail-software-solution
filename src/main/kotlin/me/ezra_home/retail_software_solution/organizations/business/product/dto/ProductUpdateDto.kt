package me.ezra_home.retail_software_solution.organizations.business.product.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductEntity}
 */
data class ProductUpdateDto (
    val id: UUID? = null,
    val productName: Optional<String>? = null,
    val description: Optional<String>? = null,
    val categoryId: Optional<UUID>? = null,
) : Serializable
