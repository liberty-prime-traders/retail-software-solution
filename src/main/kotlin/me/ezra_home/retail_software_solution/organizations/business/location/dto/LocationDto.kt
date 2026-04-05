package me.ezra_home.retail_software_solution.organizations.business.location.dto

import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationType
import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class LocationDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var locationType: LocationType? = null,
    var name: String? = null,
    var description: String? = null,
    var schemaName: String? = null,
    var timezone: String? = null
) : HasId
