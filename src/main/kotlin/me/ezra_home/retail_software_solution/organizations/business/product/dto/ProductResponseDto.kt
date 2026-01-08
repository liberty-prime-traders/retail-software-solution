package me.ezra_home.retail_software_solution.organizations.business.product.dto

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.ProductEntity}
 */
data class ProductResponseDto (
    val id: UUID?,
    val productName: String?,
    val description: String?,
    val categoryName: String?,
    val categoryId: UUID?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val baseUnit: String?,
    val baseUnitId: UUID?,
    val status: ProductStatus?,
    val activeTags: List<TagSummaryDto>? = null,
    val referenceNumber: String?
) : Serializable
