package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class StockEntryFifoDto(
    val locationProductId: UUID,
    val unitCost: BigDecimal?,
    val quantityRemaining: BigDecimal
)
