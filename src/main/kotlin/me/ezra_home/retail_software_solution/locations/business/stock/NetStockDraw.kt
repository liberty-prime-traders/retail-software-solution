package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
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

        private fun reversedMoreThanDrawn(
            stockMovementEntity: StockMovementEntity,
            excessStorageQuantity: BigDecimal
        ) = BlockedChangeException(
            "The stock records of this document do not add up, so it cannot be undone. Please contact support.",
            listOf(
                BlockingProblem(
                    documentReference = stockMovementEntity.documentReference.orEmpty(),
                    description = "Its movements on one layer reverse $excessStorageQuantity more than they drew.",
                    effect = "It cannot be voided or cancelled until support corrects its stock records."
                )
            )
        )

        // Exactly zero is a layer already reversed (a repeated void, a replaced layer) and is skipped. Below zero is
        // corrupt history and blocks the undo. Not NetConsumptionCalculator: it is layer-scoped and rejects other
        // documents' bad history on the layer.
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
                if (netQuantity.signum() < 0) throw reversedMoreThanDrawn(draws.first(), netQuantity.negate())
                if (netQuantity.signum() == 0) return@mapNotNull null
                of(draws.first()).copy(
                    storageMovedQuantity = netQuantity,
                    movedCost = draws.sumOf { it.movedCost } - reversals.sumOf { it.movedCost },
                )
            }
    }
}
