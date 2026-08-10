package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import java.math.BigDecimal
import java.util.UUID

data class StockTransferLineUpdateDto(
    val lineRef: String,
    val quantityDispatched: BigDecimal?,
    val unitId: UUID?
)
