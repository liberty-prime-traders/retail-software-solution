package me.ezra_home.retail_software_solution.platform.business.organization_join_request.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationAdminJoinRequestResponseDto(
    val id: UUID,
    val fullName: String,
    val createdById: UUID,
    val requestedDate: OffsetDateTime,
    val status: JoinRequestStatus
) : Serializable
