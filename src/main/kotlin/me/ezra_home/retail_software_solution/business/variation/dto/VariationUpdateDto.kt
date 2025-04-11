package me.ezra_home.retail_software_solution.business.variation.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.VariationEntity}
 */
data class VariationUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable
