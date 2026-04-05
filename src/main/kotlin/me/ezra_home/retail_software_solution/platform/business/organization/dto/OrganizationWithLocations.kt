package me.ezra_home.retail_software_solution.platform.business.organization.dto

import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationDto

data class OrganizationWithLocations(
    val organization: OrganizationDto,
    val locations: Collection<LocationDto>
)
