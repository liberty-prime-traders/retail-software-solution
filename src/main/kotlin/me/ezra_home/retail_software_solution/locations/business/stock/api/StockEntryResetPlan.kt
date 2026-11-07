package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.util.UUID

data class StockEntryResetPlan(
    val stockEntryId: UUID,
    val locationProductId: UUID,
    val movementDrafts: List<StockMovementDraft>,
)
