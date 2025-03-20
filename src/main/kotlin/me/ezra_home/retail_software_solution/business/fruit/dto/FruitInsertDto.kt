package me.ezra_home.retail_software_solution.business.fruit.dto

import me.ezra_home.retail_software_solution.model.entity.FruitEntity
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class FruitInsertDto(
    val name: String,
    val alternateName: String?,
    val color: String,
    val cost: BigDecimal,
    val edible: Boolean
)