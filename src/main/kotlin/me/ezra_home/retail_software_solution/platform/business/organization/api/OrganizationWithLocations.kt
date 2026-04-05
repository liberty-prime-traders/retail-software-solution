package me.ezra_home.retail_software_solution.platform.business.organization.api

import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationResponseDto
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationDto

data class OrganizationWithLocations(
    val organization: OrganizationDto,
    val locations: Collection<LocationResponseDto>
)
