package me.ezra_home.retail_software_solution.platform.business.jurisdiction.api

import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class JurisdictionDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var name: String,
    var jurisdictionTypeId: UUID,
    var parentJurisdictionId: UUID? = null
) : HasId
