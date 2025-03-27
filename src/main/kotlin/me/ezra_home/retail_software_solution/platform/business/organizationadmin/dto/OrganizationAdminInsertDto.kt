package me.ezra_home.retail_software_solution.platform.business.organizationadmin.dto

import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.platform.model.OrganizationAdminEntity}
 */
data class OrganizationAdminInsertDto(
    val organizationId: UUID? = null,
    var adminId: UUID? = null
) : Serializable
