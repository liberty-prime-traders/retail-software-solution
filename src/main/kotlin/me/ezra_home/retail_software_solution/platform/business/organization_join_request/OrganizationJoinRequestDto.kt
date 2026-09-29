package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.JoinRequestStatus
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationJoinRequestDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val subdomain: String,
    val status: JoinRequestStatus,
    val organizationId: UUID?
)
