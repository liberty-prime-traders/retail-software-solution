package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockAdjustmentCostBackfillCandidate(
    val stockAdjustmentId: UUID,
    val totalCost: BigDecimal,
    val adjustedAt: OffsetDateTime?,
)
