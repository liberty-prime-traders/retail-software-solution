package me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill

import java.util.UUID

data class StockAdjustmentBackfillResult(
    val locationId: UUID,
    val locationName: String,
    val counts: StockAdjustmentBackfillCounts,
) {
    val isReconciled: Boolean = counts.adjustmentCount == counts.activeCostRecordCount
}
