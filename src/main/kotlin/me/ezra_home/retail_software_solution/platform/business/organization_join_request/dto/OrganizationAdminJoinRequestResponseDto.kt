package me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto

import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationAdminJoinRequestResponseDto(
    val id: UUID?,
    val fullName: String,
    val requestedDate: OffsetDateTime,
    val status: JoinRequestStatus
) : Serializable
