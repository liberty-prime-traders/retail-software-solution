package me.ezra_home.retail_software_solution.organizations.business.organization_admin

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity}
 */
data class OrganizationAdminResponseDto(
    val id: UUID?,
    val user: String?,
    val createdOn: OffsetDateTime?,
    val endOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
