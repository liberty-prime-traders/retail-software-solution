package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.util.UUID

data class StockAdjustmentMovementDetails(
    val category: StockAdjustmentCategory,
    val reasonId: UUID?,
    val note: String?
)
