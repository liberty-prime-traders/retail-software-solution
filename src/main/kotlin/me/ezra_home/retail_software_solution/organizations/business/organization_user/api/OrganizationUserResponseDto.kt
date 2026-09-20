package me.ezra_home.retail_software_solution.organizations.business.organization_user.api

import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationUserResponseDto(
    val id: UUID,
    val joinRequestId: UUID?,
    val user: String?,
    val userId: UUID,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?,
    val referenceNumber: String?
)
