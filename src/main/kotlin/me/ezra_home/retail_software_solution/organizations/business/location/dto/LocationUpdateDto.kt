package me.ezra_home.retail_software_solution.organizations.business.location.dto

import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import java.io.Serializable
import java.util.Optional

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.LocationEntity}
 */
data class LocationUpdateDto(
    val locationType: Optional<LocationType>? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable
