package me.ezra_home.retail_software_solution.locations.business.location_user

import java.time.OffsetDateTime
import java.util.UUID

data class LocationUserDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val userId: UUID,
    val endOn: OffsetDateTime? = null
) {
    fun isActive(): Boolean = endOn == null
}
