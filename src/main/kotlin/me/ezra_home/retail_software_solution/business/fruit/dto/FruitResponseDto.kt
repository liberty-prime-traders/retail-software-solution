package me.ezra_home.retail_software_solution.business.fruit.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitResponseDTO(
    val id: UUID?,
    val name: String,
    val alternateName: String?,
    val color: String?,
    val cost: Double?,
    val edibleInd: Boolean,
    val createdById: UUID?,
    val createdOn: OffsetDateTime?,
)