package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import java.time.Instant
import java.util.UUID

// quantityMoved and newQuantity are stored breakdown texts, never re-rendered; quantityEntered is the whole source line's figure, repeated on every movement it produced
data class StockMovementResponse(
    val id: UUID,
    val movementType: MovementType,
    val locationProductId: UUID,
    val documentReference: String?,
    val detailReference: String?,
    val quantityMoved: String,
    val quantityEntered: String,
    val newQuantity: String,
    val stockEntryId: UUID,
    val recordedOn: Instant,
    val conversionDriftNote: String?,
    val reason: String?
)
