package me.ezra_home.retail_software_solution.organizations.business.product.dto

import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductEntity}
 */
data class ProductInsertDto(
    val productName: String,
    val description: String? = null,
    val categoryId: UUID? = null,
    val baseUnitId: UUID
) : Serializable
