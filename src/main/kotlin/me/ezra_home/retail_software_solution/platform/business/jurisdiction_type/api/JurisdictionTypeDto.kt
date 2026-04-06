package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api

import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionTypeDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var name: String,
    var description: String? = null
)
