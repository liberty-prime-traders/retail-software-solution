package me.ezra_home.retail_software_solution.fruit.dto

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import java.util.Locale
import java.math.BigDecimal
import java.util.UUID


/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val alternateName: Optional<String>? = null,
    val color: Optional<String>? = null,
    val cost: Optional<BigDecimal>? = null,
    val removalCost: Optional<BigDecimal>? = null,
    val edible: Optional<Boolean>? = null
) : Serializable

