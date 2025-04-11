package me.ezra_home.retail_software_solution.business.variation.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.VariationEntity}
 */
data class VariationResponseDto(
    val id: UUID?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?,
    val name: String?,
    val description: String?
) : Serializable
