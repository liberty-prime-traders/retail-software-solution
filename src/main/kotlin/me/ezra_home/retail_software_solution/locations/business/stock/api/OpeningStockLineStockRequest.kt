package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class OpeningStockLineStockRequest(
    val locationProductId: UUID,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
    val unitCost: BigDecimal,
    val documentReference: String
)
