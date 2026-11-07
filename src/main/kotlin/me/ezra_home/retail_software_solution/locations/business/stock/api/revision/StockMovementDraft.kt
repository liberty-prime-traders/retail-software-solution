package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class StockMovementDraft(
    val movementType: MovementType,
    val storageMoved: ExactQuantity,
    val balanceAfterMovement: ExactQuantity,
    val movedCost: BigDecimal,
    val wholeLineQuantityText: String,
    val documentReference: String?,
    val detailReference: String?,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val context: String,
    val stockRevisionReference: String,
)
