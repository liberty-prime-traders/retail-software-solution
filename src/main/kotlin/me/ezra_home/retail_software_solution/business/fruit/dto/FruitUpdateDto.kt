package me.ezra_home.retail_software_solution.business.fruit.dto

import java.io.Serializable
import java.util.*

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */

data class FruitUpdateDTO (
    val id: UUID? = null,
    val name: Optional<String>?,
    val alternateName: Optional<String>?,
    val color: Optional<String>?,
    val cost: Optional<Double>?,
    val edibleInd: Optional<Boolean>?
) : Serializable
