package me.ezra_home.retail_software_solution.platform.business.organization_join_request

import me.ezra_home.retail_software_solution.platform.business.organization_join_request.api.JoinRequestStatus
import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationJoinRequestDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var subdomain: String,
    var status: JoinRequestStatus,
    var organizationId: UUID?
) : HasId
