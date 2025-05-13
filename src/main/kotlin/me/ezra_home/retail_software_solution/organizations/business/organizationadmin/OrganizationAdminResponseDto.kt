package me.ezra_home.retail_software_solution.organizations.business.organizationadmin

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.OrganizationAdminEntity}
 */
data class OrganizationAdminResponseDto(
    val id: UUID?,
    val admin: String?,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?
) : Serializable
