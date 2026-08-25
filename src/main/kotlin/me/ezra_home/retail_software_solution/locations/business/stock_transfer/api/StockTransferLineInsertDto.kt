package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import java.math.BigDecimal
import java.util.UUID

data class StockTransferLineInsertDto(
    val locationProductId: UUID,
    val quantityDispatched: BigDecimal
)
