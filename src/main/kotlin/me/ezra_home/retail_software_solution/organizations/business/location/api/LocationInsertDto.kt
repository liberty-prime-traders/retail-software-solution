package me.ezra_home.retail_software_solution.organizations.business.location.api


data class LocationInsertDto(
    val locationType: LocationType? = null,
    val name: String? = null,
    val description: String? = null,
    val timezone: String = "Africa/Addis_Ababa"
)
