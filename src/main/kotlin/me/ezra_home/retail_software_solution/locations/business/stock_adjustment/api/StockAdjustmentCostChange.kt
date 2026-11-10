package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import java.math.BigDecimal

data class StockAdjustmentCostChange(
    val adjustmentReferenceNumber: String,
    val oldCost: BigDecimal,
    val newCost: BigDecimal,
)
