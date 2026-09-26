package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class OpeningStockLineStockRequest(
    val locationProductId: UUID,
    val quantity: BigDecimal,
    val unitId: UUID,
    val unitCost: BigDecimal,
    val externalReferenceNumber: String
)
