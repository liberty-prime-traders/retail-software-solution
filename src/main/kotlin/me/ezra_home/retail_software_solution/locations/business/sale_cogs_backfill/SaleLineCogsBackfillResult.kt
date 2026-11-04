package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill

import java.util.UUID

data class SaleLineCogsBackfillResult(
    val locationId: UUID,
    val locationName: String,
    val counts: SaleLineCogsBackfillCounts,
) {
    val isReconciled: Boolean = counts.saleLinesWithCogsTotalCount == counts.costRecordCount
}
