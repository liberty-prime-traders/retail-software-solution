package me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill

data class StockAdjustmentBackfillCounts(
    val insertedCostRecordCount: Long,
    val adjustmentCount: Long,
    val activeCostRecordCount: Long,
)
