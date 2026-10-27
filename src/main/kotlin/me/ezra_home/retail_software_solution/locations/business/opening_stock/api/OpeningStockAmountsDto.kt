package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.math.BigDecimal
import java.util.UUID

data class OpeningStockAmountsDto(
    val userProvidedQuantity: PositiveUserProvidedQuantity,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val unitCost: BigDecimal
)
