package me.ezra_home.retail_software_solution.business.location.dto

import me.ezra_home.retail_software_solution.model.enums.LocationType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.LocationEntity}
 */
data class LocationResponseDto(
    val id: UUID?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long,
    val organizationId: UUID?,
    val locationType: LocationType?,
    val name: String?,
    val description: String?
) : Serializable
