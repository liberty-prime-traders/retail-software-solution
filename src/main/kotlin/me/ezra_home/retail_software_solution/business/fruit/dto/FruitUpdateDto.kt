package me.ezra_home.retail_software_solution.business.fruit.dto

import java.io.Serializable

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */

data class FruitUpdateDTO(
    val name: String?,
    val alternateName: String?,
    val color: String?,
    val cost: Double?,
    val edibleInd: Boolean?
) : Serializable
