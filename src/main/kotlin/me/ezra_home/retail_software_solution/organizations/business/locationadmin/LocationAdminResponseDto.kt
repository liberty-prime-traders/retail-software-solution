package me.ezra_home.retail_software_solution.organizations.business.locationadmin

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.LocationAdminEntity}
 */
data class LocationAdminResponseDto(
    val id: UUID?,
    val admin: String?,
    val startOn: OffsetDateTime?,
    val endOn: OffsetDateTime?
) : Serializable
