package me.ezra_home.retail_software_solution.platform.business.organization.dto

import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity

data class OrganizationWithLocations(
    val organization: OrganizationEntity,
    val locations: Collection<LocationEntity>
)
