package me.ezra_home.retail_software_solution.business.userlocation.dto

import java.io.Serializable
import java.util.UUID

data class UserLocationResponseDto (
    val locationId: UUID,
    val users: Collection<LocationUserDto> = ArrayList()
): Serializable
