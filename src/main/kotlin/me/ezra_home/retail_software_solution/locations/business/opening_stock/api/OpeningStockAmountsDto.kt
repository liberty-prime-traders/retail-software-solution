package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import java.math.BigDecimal
import java.util.UUID

data class OpeningStockAmountsDto(
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
    val unitCost: BigDecimal
)
