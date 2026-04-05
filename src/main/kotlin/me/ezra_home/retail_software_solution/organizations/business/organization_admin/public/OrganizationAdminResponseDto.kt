package me.ezra_home.retail_software_solution.organizations.business.organization_admin.public

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationAdminResponseDto(
    val id: UUID,
    val user: String?,
    val userId: UUID,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
