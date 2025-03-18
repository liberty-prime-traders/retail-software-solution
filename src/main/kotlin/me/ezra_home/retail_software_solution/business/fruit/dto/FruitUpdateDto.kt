package me.ezra_home.retail_software_solution.fruit.dto

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import java.text.NumberFormat
import java.util.Locale


data class FruitUpdateDto(
    val name: String?,
    val alternateName: String?,
    val color: String?,
    val cost: BigDecimal?,
    val edible: Boolean?
)
