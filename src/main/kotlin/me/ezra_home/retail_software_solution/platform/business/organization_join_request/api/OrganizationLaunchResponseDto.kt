package me.ezra_home.retail_software_solution.platform.business.organization_join_request.api

import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationResponseDto
import java.io.Serializable

data class OrganizationLaunchResponseDto(
    val organization: OrganizationResponseDto? = null,
    val isOrganizationAdmin: Boolean,
    val accessRequested: Boolean
) : Serializable
