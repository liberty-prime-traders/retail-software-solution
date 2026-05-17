package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class StockEntryPreview(
    val id: UUID,
    val locationProductId: UUID,
    val externalReferenceNumber: String?,
    val priority: Int?,
    val availableBaseQty: BigDecimal,
    val unitCost: BigDecimal?
)
