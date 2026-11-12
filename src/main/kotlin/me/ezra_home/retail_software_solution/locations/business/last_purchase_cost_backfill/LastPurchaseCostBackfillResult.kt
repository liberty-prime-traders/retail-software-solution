package me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill

import java.util.UUID

data class LastPurchaseCostBackfillResult(
    val locationId: UUID,
    val locationName: String,
    val updatedProductCount: Long,
)
