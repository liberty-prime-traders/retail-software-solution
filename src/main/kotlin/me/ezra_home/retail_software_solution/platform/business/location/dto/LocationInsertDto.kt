package me.ezra_home.retail_software_solution.platform.business.location.dto

import me.ezra_home.retail_software_solution.util.enums.LocationType
import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.LocationEntity}
 */
data class LocationInsertDto(
    val locationType: LocationType? = null,
    val name: String? = null,
    val description: String? = null
) : Serializable
