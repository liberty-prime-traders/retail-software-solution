package me.ezra_home.retail_software_solution.locations.business.stock.api.read

import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockMovementDto(
    val id: UUID?,
    val createdOn: OffsetDateTime?,
    val stockEntryId: UUID,
    val locationProductId: UUID,
    val movementType: MovementType,
    val storageMovedQuantity: BigDecimal,
    val movedCost: BigDecimal,
    val documentReference: String?,
    val detailReference: String?,
    val adjustmentDetails: StockAdjustmentMovementDetails?,
    val quantityAsEntered: QuantityAsEntered,
) {
    companion object {
        fun of(stockMovementEntity: StockMovementEntity) = StockMovementDto(
            id = stockMovementEntity.id,
            createdOn = stockMovementEntity.createdOn,
            stockEntryId = stockMovementEntity.stockEntryId,
            locationProductId = stockMovementEntity.locationProductId,
            movementType = stockMovementEntity.movementType,
            storageMovedQuantity = stockMovementEntity.storageMovedQuantity,
            movedCost = stockMovementEntity.movedCost,
            documentReference = stockMovementEntity.documentReference,
            detailReference = stockMovementEntity.detailReference,
            adjustmentDetails = stockMovementEntity.category?.let {
                StockAdjustmentMovementDetails(it, stockMovementEntity.reasonId, stockMovementEntity.note)
            },
            quantityAsEntered = stockMovementEntity.quantityAsEntered(),
        )
    }
}
