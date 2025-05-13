package me.ezra_home.retail_software_solution.platform.business.organization_join_request.dto

import me.ezra_home.retail_software_solution.util.enums.JoinRequestStatus
import java.io.Serializable
import java.time.OffsetDateTime

data class OrganizationJoinRequestResponseDto(
    val domain: String,
    val requestedDate: OffsetDateTime,
    val status: JoinRequestStatus
) : Serializable
