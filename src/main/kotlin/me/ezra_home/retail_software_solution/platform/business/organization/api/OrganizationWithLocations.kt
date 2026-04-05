package me.ezra_home.retail_software_solution.platform.business.organization.api

import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationDto

data class OrganizationWithLocations(
    val organization: OrganizationDto,
    val locations: Collection<LocationDto>
)
