package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill

import java.util.UUID

data class PurchaseMovementContextBatch(
    val updatedMovementCount: Int,
    val lastStockMovementId: UUID,
)
