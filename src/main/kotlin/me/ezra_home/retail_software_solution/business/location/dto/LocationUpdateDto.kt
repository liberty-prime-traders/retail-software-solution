package me.ezra_home.retail_software_solution.business.location.dto

import me.ezra_home.retail_software_solution.model.enums.LocationType
import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.LocationEntity}
 */
data class LocationUpdateDto(
    val id: UUID? = null,
    val organizationId: Optional<UUID>? = null,
    val locationType: Optional<LocationType>? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable
