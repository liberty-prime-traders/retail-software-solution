package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api

import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionTypeDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val name: String,
    val description: String? = null
)
