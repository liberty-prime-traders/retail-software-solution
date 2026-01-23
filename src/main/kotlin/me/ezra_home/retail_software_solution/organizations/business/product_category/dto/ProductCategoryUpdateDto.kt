package me.ezra_home.retail_software_solution.organizations.business.product_category.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductCategoryEntity}
 */
data class ProductCategoryUpdateDto (
    val id: UUID? = null,
    val categoryName: Optional<String>? = null,
    val description: Optional<String>? = null,
) : Serializable
