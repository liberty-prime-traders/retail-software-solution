package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import java.math.BigDecimal
import java.util.UUID

data class StockReplacementLayerRequest(
    val locationProductId: UUID,
    val sourceType: StockItemSource,
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String,
    val stockRevisionReferenceNumber: String,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val batchCost: BigDecimal,
    val revisedStockEntryId: UUID,
)
