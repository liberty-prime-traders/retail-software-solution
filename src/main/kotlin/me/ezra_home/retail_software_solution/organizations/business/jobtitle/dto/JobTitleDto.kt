package me.ezra_home.retail_software_solution.organizations.business.jobtitle.dto

import java.time.OffsetDateTime
import java.util.UUID

data class JobTitleDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var value: String? = null
)
