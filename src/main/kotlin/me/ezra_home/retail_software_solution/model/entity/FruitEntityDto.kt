package me.ezra_home.retail_software_solution.model.entity

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.*

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitEntityDto(
    val id: UUID? = null,
    @field:NotNull @field:Size(max = 255) val name: String? = null,
    @field:Size(max = 255) val alternateName: String? = null,
    @field:NotNull @field:Size(max = 50) val color: String? = null,
    @field:NotNull val cost: BigDecimal? = null,
    @field:NotNull val edibleInd: Boolean? = false,
    @field:NotNull val createdById: UUID? = null,
    val createdOn: OffsetDateTime? = null
) : Serializable