package me.ezra_home.retail_software_solution.business.fruit.dto

import java.io.Serializable
import java.math.BigDecimal
import java.util.*


/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val fruitName: Optional<String>? = null,
    val color: Optional<String>? = null,
    val cost: Optional<BigDecimal>? = null,
    val edible: Optional<Boolean>? = null
) : Serializable


