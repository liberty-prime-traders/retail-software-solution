package me.ezra_home.retail_software_solution.business.variation.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.*

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.VariationEntity}
 */
data class VariationDto(
    val id: Long? = null,
    @field:NotNull @field:Size(max = 255) val name: String? = null,
    val description: String? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    @field:NotNull val createdBy: UUID? = null,
    val updatedBy: UUID? = null,
    @field:NotNull val isActive: Boolean? = false
) : Serializable