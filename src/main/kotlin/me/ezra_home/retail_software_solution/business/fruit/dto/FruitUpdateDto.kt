package me.ezra_home.retail_software_solution.fruit.dto

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import java.util.Locale
import java.math.BigDecimal
import java.util.UUID


/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.FruitEntity}
 */
data class FruitUpdateDto(
    val id: UUID?,
    val name: String?,
    val alternateName: String?,
    val color: String?,
    val cost: BigDecimal?,
    val edible: Boolean?
)
