package me.ezra_home.retail_software_solution.business.variation.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.VariationEntity}
 */
data class VariationCreateDto(
    @field:NotNull @field:Size(max = 255) val name: String? = null,
    val description: String? = null,
    @field:NotNull val isActive: Boolean? = false
) : Serializable