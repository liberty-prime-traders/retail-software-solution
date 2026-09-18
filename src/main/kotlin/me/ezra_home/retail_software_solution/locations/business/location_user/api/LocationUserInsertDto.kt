package me.ezra_home.retail_software_solution.locations.business.location_user.api

import java.io.Serializable
import java.util.UUID

data class LocationUserInsertDto(
    val userId: UUID
) : Serializable
