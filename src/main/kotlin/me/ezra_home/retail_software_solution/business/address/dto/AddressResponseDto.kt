package me.ezra_home.retail_software_solution.business.address.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.AddressEntity}
 */
data class AddressResponseDto(
    val id: UUID?,
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?
) : Serializable
