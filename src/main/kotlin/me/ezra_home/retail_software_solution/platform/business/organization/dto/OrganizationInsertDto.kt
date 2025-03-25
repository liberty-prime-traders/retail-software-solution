package me.ezra_home.retail_software_solution.platform.business.organization.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.platform.model.OrganizationEntity}
 */
data class OrganizationInsertDto(
    val name: String? = null,
    val description: String? = null,
    val subdomain: String? = null
) : Serializable
