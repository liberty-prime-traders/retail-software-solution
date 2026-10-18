package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class StockAdjustmentDecreaseStockRequest(
    val locationProductId: UUID,
    val productLabel: String,
    val storageQuantity: ExactQuantity,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
    val documentReference: String,
    val adjustmentDetails: StockAdjustmentMovementDetails,
)
