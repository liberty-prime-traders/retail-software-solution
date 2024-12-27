package me.ezra_home.retail_software_solution.business.organization.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.OrganizationEntity}
 */
data class OrganizationInsertDto(
    val name: String,
    val description: String? = null
) : Serializable
