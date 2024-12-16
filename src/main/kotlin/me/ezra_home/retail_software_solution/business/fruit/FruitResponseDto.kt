package me.ezra_home.retail_software_solution.business.fruit

import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitResponseDto(
    val id: UUID? = null,
    val name: String? = null,
    val alternateName: String? = null,
    val color: String? = null,
    val cost: BigDecimal? = null,
    val edibleInd: Boolean? = null,
    val createdBy: String? = null,
    val createdOn: OffsetDateTime? = null
) : Serializable