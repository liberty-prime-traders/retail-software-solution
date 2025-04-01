package me.ezra_home.retail_software_solution.platform.business.organization.dto

import java.io.Serializable
import java.util.Optional

/**
 * DTO for {@link me.ezra_home.retail_software_solution.platform.model.OrganizationEntity}
 */
data class OrganizationUpsertDto (
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val subdomain: String? = null
) : Serializable
