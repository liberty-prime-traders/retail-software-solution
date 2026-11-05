package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill

data class PurchaseMovementContextBackfillCounts(
    val updatedMovementCount: Long,
    val movementsStillMissingContextCount: Long,
)
