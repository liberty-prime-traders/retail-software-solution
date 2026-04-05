package me.ezra_home.retail_software_solution.organizations.business.location.api

import java.io.Serializable
import java.util.Optional

data class LocationUpdateDto(
    val locationType: Optional<LocationType>? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val timezone: Optional<String>? = null
) : Serializable
