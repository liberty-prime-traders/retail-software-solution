package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationProductDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val productName: String,
    val description: String? = null,
    val productGroupId: UUID,
    val productGroupName: String? = null,
    val baseUnitId: UUID,
    val status: ProductStatus? = ProductStatus.ACTIVE
)
