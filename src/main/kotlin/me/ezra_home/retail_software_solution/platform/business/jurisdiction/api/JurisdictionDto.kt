package me.ezra_home.retail_software_solution.platform.business.jurisdiction.api

import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val name: String,
    val jurisdictionTypeId: UUID,
    val parentJurisdictionId: UUID? = null
)
