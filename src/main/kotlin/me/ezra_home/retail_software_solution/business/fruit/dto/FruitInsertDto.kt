package me.ezra_home.retail_software_solution.business.fruit.dto

import java.math.BigDecimal

data class FruitInsertDto(
    val name: String,
    val alternateName: String?,
    val color: String,
    val cost: BigDecimal,
    val edible: Boolean
)