package me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity}
 */
data class OrganizationAdminResponseDto(
    val id: UUID?,
    val adminId: UUID?,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?
) : Serializable
