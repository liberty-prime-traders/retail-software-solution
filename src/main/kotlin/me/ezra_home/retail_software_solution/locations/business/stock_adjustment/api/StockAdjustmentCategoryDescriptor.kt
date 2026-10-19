package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import java.util.UUID

data class StockAdjustmentCategoryDescriptor(
    val category: StockAdjustmentCategory,
    val displayName: String,
    val allowedDirections: List<StockAdjustmentDirection>,
    val requiresUnitCost: Boolean,
    val requiresReason: Boolean,
    val requiresNote: Boolean,
    val reasons: List<StockAdjustmentReasonOption>
)

data class StockAdjustmentReasonOption(
    val id: UUID,
    val name: String,
    val allowedDirections: List<StockAdjustmentDirection>
)
