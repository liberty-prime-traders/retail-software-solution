package me.ezra_home.retail_software_solution.organizations.business.product.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationProductResponseDto(
    val id: UUID,
    val productName: String?,
    val description: String?,
    val categoryName: String?,
    val categoryId: UUID?,
    val productGroupId: UUID?,
    val productGroupName: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val baseUnit: String?,
    val baseUnitId: UUID?,
    val status: ProductStatus?,
    val activeTags: List<TagSummaryDto>? = null,
    val referenceNumber: String?
) : Serializable
