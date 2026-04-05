package me.ezra_home.retail_software_solution.organizations.business.product_tag

import java.time.OffsetDateTime
import java.util.UUID

data class ProductTagDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var endOn: OffsetDateTime? = null,
    var productId: UUID,
    var tagId: UUID
)
