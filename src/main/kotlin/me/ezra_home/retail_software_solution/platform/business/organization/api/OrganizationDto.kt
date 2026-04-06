package me.ezra_home.retail_software_solution.platform.business.organization.api

import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var name: String,
    var description: String? = null,
    var hidden: Boolean = false,
    var currentDbVersionId: UUID? = null,
    var creationPassId: UUID? = null,
    var subdomain: String? = null,
    var schemaName: String? = null,
    var timezone: String? = null
) : HasId
