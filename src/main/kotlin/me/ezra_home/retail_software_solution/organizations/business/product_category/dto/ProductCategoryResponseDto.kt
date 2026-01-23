package me.ezra_home.retail_software_solution.organizations.business.product_category.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductCategoryEntity}
 */
data class ProductCategoryResponseDto (
    val id: UUID?,
    val categoryName: String?,
    val description: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
