package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import java.math.BigDecimal
import java.util.UUID

data class NetStockDraw(
    val stockEntryId: UUID,
    val locationProductId: UUID,
    val documentReference: String?,
    val detailReference: String?,
    val storageMovedQuantity: BigDecimal,
    val movedCost: BigDecimal,
    val wholeLineQuantityText: String,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
) {

    companion object {

        fun of(stockMovementEntity: StockMovementEntity) = NetStockDraw(
            stockEntryId = stockMovementEntity.stockEntryId,
            locationProductId = stockMovementEntity.locationProductId,
            documentReference = stockMovementEntity.documentReference,
            detailReference = stockMovementEntity.detailReference,
            storageMovedQuantity = stockMovementEntity.storageMovedQuantity,
            movedCost = stockMovementEntity.movedCost,
            wholeLineQuantityText = stockMovementEntity.wholeLineQuantityText,
            unitId = stockMovementEntity.unitId,
            storageUnitsPerUserProvidedUnit = stockMovementEntity.storageUnitsPerUserProvidedUnit,
        )

        // A group at zero or below is skipped, not failed, so a document with an odd history can still be voided.
        // Not NetConsumptionCalculator: it is layer-scoped and rejects other documents' bad history on the layer.
        fun netOf(
            stockMovementEntities: List<StockMovementEntity>,
            drawType: MovementType
        ): List<NetStockDraw> = stockMovementEntities
            .filter { it.movementType in drawType.withReversalTypes() }
            .groupBy { Triple(it.stockEntryId, it.documentReference, it.detailReference) }
            .values
            .mapNotNull { movementsOfOneLineOnOneLayer ->
                val (draws, reversals) = movementsOfOneLineOnOneLayer.partition { it.movementType == drawType }
                if (draws.isEmpty()) return@mapNotNull null
                val netQuantity = draws.sumOf { it.storageMovedQuantity } - reversals.sumOf { it.storageMovedQuantity }
                if (netQuantity.signum() <= 0) return@mapNotNull null
                of(draws.first()).copy(
                    storageMovedQuantity = netQuantity,
                    movedCost = draws.sumOf { it.movedCost } - reversals.sumOf { it.movedCost },
                )
            }
    }
}
