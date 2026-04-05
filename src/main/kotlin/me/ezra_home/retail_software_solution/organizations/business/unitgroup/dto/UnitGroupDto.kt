package me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto

import java.time.OffsetDateTime
import java.util.UUID

data class UnitGroupDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var name: String? = null,
    var description: String? = null
)
