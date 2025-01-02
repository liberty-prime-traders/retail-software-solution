package me.ezra_home.retail_software_solution.business.fruit.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */

data class FruitInsertDTO (
    val name: String,
    val alternateName: String?  = null,
    val color: String? = null,
    val cost: Double? = null,
    val edibleInd: Boolean
) : Serializable

