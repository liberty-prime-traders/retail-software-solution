package me.ezra_home.retail_software_solution.locations.business.variation.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.locations.model.VariationEntity}
 */
data class VariationInsertDto(
    val name: String? = null,
    val description: String? = null
) : Serializable
