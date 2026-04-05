package me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto

import java.time.OffsetDateTime
import java.util.UUID

data class UnitValueDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var name: String? = null,
    var description: String? = null,
    var code: String? = null,
    var unitGroupId: UUID? = null,
    var baseUnit: UUID? = null,
    var conversionFactor: Double? = null
)
