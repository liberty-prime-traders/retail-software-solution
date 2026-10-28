package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.FifoTakePlanner
import me.ezra_home.retail_software_solution.locations.business.stock.api.PlannedTake
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object FifoStockConsumer {

    fun groupInFifoOrder(stockEntries: List<StockEntryEntity>): Map<UUID, List<StockEntryEntity>> =
        stockEntries
            .groupBy { it.locationProductId }
            .mapValues { (_, stockEntriesForProduct) -> stockEntriesForProduct.sortedWith(stockEntryFifoComparator) }

    fun consume(
        fifoConsumptionRequest: FifoConsumptionRequest,
        fifoEntries: List<StockEntryEntity>,
        startingBalance: BigDecimal,
        quantityBreakdown: QuantityBreakdown,
        productLabel: String
    ): FifoConsumptionResult {
        val plannedTakes = FifoTakePlanner.planTakes(fifoEntries, fifoConsumptionRequest.storageQuantity)
        throwIfNotFulfilled(fifoConsumptionRequest, plannedTakes, quantityBreakdown, productLabel)
        var runningBalance = ExactQuantity.ofStorage(startingBalance)
        var consumedCost = BigDecimal.ZERO
        val touchedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        plannedTakes.forEach { plannedTake ->
            val stockEntry = fifoEntries[plannedTake.layerIndex]
            stockEntry.storageQuantityRemaining =
                (ExactQuantity.ofStorage(stockEntry.storageQuantityRemaining) - plannedTake.storageQuantity).toStorageQuantity()
            stockEntry.costRemaining = stockEntry.costRemaining.subtract(plannedTake.movedCost)
            touchedEntries.add(stockEntry)
            runningBalance -= plannedTake.storageQuantity
            movements.add(
                StockMovementCreator.create(
                    stockEntryId = stockEntry.id!!,
                    locationProductId = fifoConsumptionRequest.locationProductId,
                    movementType = fifoConsumptionRequest.movementType,
                    storageMoved = plannedTake.storageQuantity,
                    balanceAfterMovement = runningBalance,
                    movedCost = plannedTake.movedCost,
                    wholeLineQuantityText = quantityBreakdown.renderUserProvided(fifoConsumptionRequest.userProvidedQuantity, fifoConsumptionRequest.unitId),
                    documentReference = fifoConsumptionRequest.documentReference,
                    detailReference = fifoConsumptionRequest.detailReference,
                    unitId = fifoConsumptionRequest.unitId,
                    storageUnitsPerUserProvidedUnit = fifoConsumptionRequest.storageUnitsPerUserProvidedUnit,
                    quantityBreakdown = quantityBreakdown,
                    adjustmentDetails = fifoConsumptionRequest.adjustmentDetails,
                )
            )
            consumedCost = consumedCost.add(plannedTake.movedCost)
        }
        return FifoConsumptionResult(movements, touchedEntries, consumedCost, runningBalance.toStorageQuantity())
    }

    fun restore(
        movementsToReverse: List<StockMovementEntity>,
        stockEntriesById: Map<UUID, StockEntryEntity>,
        startingBalance: BigDecimal,
        quantityBreakdown: QuantityBreakdown,
        reversalType: MovementType
    ): FifoRestorationResult {
        var runningBalance = ExactQuantity.ofStorage(startingBalance)
        val touchedEntries = mutableListOf<StockEntryEntity>()
        val reversalMovements = mutableListOf<StockMovementEntity>()
        movementsToReverse.forEach { movementToReverse ->
            val stockEntry = stockEntriesById.getValue(movementToReverse.stockEntryId)
            val restoredQuantity = ExactQuantity.ofStorage(movementToReverse.storageMovedQuantity)
            stockEntry.storageQuantityRemaining = (ExactQuantity.ofStorage(stockEntry.storageQuantityRemaining) + restoredQuantity).toStorageQuantity()
            stockEntry.costRemaining = stockEntry.costRemaining.add(movementToReverse.movedCost)
            touchedEntries.add(stockEntry)
            runningBalance += restoredQuantity
            reversalMovements.add(
                StockMovementCreator.create(
                    stockEntryId = movementToReverse.stockEntryId,
                    locationProductId = movementToReverse.locationProductId,
                    movementType = reversalType,
                    storageMoved = restoredQuantity,
                    balanceAfterMovement = runningBalance,
                    movedCost = movementToReverse.movedCost,
                    wholeLineQuantityText = movementToReverse.wholeLineQuantityText,
                    documentReference = movementToReverse.documentReference,
                    detailReference = movementToReverse.detailReference,
                    unitId = movementToReverse.unitId,
                    storageUnitsPerUserProvidedUnit = movementToReverse.storageUnitsPerUserProvidedUnit,
                    quantityBreakdown = quantityBreakdown,
                )
            )
        }
        return FifoRestorationResult(reversalMovements, touchedEntries, runningBalance.toStorageQuantity())
    }

    private fun throwIfNotFulfilled(
        fifoConsumptionRequest: FifoConsumptionRequest,
        plannedTakes: List<PlannedTake>,
        quantityBreakdown: QuantityBreakdown,
        productLabel: String
    ) {
        val availableQuantity = plannedTakes.fold(ExactQuantity.ZERO) { sum, plannedTake -> sum + plannedTake.storageQuantity }
        if (availableQuantity >= fifoConsumptionRequest.storageQuantity) return
        throw RtsGenericException(
            "Insufficient stock for $productLabel. Available: ${quantityBreakdown.render(availableQuantity)}, " +
                "Requested: ${quantityBreakdown.render(fifoConsumptionRequest.storageQuantity)}"
        )
    }
}
