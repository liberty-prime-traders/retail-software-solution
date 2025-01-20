package me.ezra_home.retail_software_solution.business.userlocation.dto

import java.io.Serializable
import java.util.UUID

data class UserLocationRequestDto (
    val locationId: UUID?,
    val usersToAdd: Collection<UUID>?,
    val usersToRemove: Collection<UUID>?
): Serializable
