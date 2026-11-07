package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill

import java.util.UUID

data class PurchaseMovementContextBackfillResult(
    val locationId: UUID,
    val locationName: String,
    val counts: PurchaseMovementContextBackfillCounts,
) {
    val isComplete: Boolean = counts.movementsStillMissingContextCount == 0L
}
