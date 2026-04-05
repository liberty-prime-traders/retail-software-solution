package me.ezra_home.retail_software_solution.organizations.business.location.api

import java.io.Serializable

data class LocationInsertDto(
    val locationType: LocationType? = null,
    val name: String? = null,
    val description: String? = null,
    val timezone: String? = null
) : Serializable
