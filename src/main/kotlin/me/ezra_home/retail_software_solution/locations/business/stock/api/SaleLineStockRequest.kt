package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class SaleLineStockRequest(
    val saleLineId: UUID,
    val locationProductId: UUID,
    val baseQuantity: BigDecimal,
    val unitId: UUID,
    val conversionFactor: BigDecimal
)
