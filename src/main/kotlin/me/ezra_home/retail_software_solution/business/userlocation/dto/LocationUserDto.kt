package me.ezra_home.retail_software_solution.business.userlocation.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class LocationUserDto (
    val fullName: String?,
    val userId: UUID?,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?
): Serializable
