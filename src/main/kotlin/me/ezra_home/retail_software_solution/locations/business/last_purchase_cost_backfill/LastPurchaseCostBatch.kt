package me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill

import java.util.UUID

data class LastPurchaseCostBatch(
    val updatedProductCount: Int,
    val lastLocationProductId: UUID,
)
