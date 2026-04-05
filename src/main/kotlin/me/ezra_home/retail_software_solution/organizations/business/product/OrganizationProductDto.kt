package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationProductDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var productName: String,
    var description: String? = null,
    var productGroupId: UUID,
    var productGroupName: String? = null,
    var baseUnitId: UUID,
    var status: ProductStatus? = ProductStatus.ACTIVE
)
