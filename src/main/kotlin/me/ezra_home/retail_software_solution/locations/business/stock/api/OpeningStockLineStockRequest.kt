package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.math.BigDecimal
import java.util.UUID

data class OpeningStockLineStockRequest(
    val locationProductId: UUID,
    val userProvidedQuantity: PositiveUserProvidedQuantity,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val unitCost: BigDecimal,
    val documentReference: String
)
