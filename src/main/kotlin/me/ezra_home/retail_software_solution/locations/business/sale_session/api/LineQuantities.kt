package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal

data class LineQuantities(
    val userProvidedQuantity: BigDecimal,
    val storageUnitsPerUserProvidedUnit: Long,
    val storageUnitsPerBaseUnit: Long
)
