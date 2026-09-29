package me.ezra_home.retail_software_solution.organizations.business.location.api

import java.time.OffsetDateTime
import java.util.UUID

data class LocationDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val locationType: LocationType? = null,
    val name: String? = null,
    val description: String? = null,
    val schemaName: String? = null,
    val timezone: String? = null
)
