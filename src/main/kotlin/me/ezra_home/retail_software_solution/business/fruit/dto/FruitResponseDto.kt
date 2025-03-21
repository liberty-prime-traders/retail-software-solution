package me.ezra_home.retail_software_solution.business.fruit.dto

import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitResponseDto(
    val id: UUID?,
    val name: String?,
    val alternateName: String?,
    val color: String?,
    val cost: BigDecimal?,
    val edible: Boolean?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?
) : Serializable

