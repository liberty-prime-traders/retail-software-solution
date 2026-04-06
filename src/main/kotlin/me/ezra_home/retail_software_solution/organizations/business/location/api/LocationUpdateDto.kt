package me.ezra_home.retail_software_solution.organizations.business.location.api

import java.io.Serializable
import java.util.Optional

data class LocationUpdateDto(
    val locationType: Optional<LocationType>? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val timezone: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: LocationDto): LocationDto = existing.copy(
        locationType = locationType?.orElse(existing.locationType) ?: existing.locationType,
        name = name?.orElse(existing.name) ?: existing.name,
        description = description?.orElse(existing.description) ?: existing.description,
        timezone = timezone?.orElse(existing.timezone) ?: existing.timezone
    )
}
