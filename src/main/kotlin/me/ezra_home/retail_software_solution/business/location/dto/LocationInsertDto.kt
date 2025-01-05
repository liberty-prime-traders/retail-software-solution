package me.ezra_home.retail_software_solution.business.location.dto

import me.ezra_home.retail_software_solution.model.enums.LocationType
import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.LocationEntity}
 */
data class LocationInsertDto(
    val organizationId: UUID? = null,
    val locationType: LocationType? = null,
    val name: String? = null,
    val description: String? = null
) : Serializable
