package me.ezra_home.retail_software_solution.platform.business.organization.api

import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val name: String,
    val description: String? = null,
    val hidden: Boolean = false,
    val currentDbVersionId: UUID? = null,
    val creationPassId: UUID? = null,
    val subdomain: String? = null,
    val schemaName: String? = null,
    val timezone: String? = null
)
