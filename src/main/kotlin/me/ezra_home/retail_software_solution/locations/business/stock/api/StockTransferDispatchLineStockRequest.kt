package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class StockTransferDispatchLineStockRequest(
    val dispatchLineRef: String,
    val locationProductId: UUID,
    val baseQuantity: BigDecimal,
    val unitId: UUID,
    val unitCost: BigDecimal,
    val conversionFactor: BigDecimal,
    val baseUnitId: UUID
)
