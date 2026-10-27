package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class FifoConsumptionRequest(
    val locationProductId: UUID,
    val storageQuantity: ExactQuantity,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String?,
    val adjustmentDetails: StockAdjustmentMovementDetails? = null,
)
