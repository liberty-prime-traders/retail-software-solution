package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.time.Instant
import java.util.UUID

data class StockMovementResponse(
    val id: UUID,
    val movementType: MovementType,
    val locationProductId: UUID,
    val externalReferenceNumber: String?,
    val quantityMoved: String,
    val newQuantity: String,
    val recordedOn: Instant,
    val conversionDriftNote: String?,
    val reason: String?
)
