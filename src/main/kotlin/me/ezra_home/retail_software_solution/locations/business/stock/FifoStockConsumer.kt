package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
        productLabel: String
    ): FifoConsumptionResult {
        var runningBalance = startingBalance
        var unsatisfiedQuantity = fifoConsumptionRequest.baseQuantity
        var consumedCost = BigDecimal.ZERO
        var movedQuantityLeft = fifoConsumptionRequest.enteredQuantity
        val touchedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        val conversionRatio = fifoConsumptionRequest.conversionRatio
        for (stockEntry in fifoEntries) {
            if (unsatisfiedQuantity <= BigDecimal.ZERO) break
            val takenQuantityInBaseUnit = unsatisfiedQuantity.min(stockEntry.quantityRemaining)
            val takesTheRest = takenQuantityInBaseUnit.compareTo(unsatisfiedQuantity) == 0
            val movedQuantity = if (takesTheRest) movedQuantityLeft else conversionRatio.invert().applyTo(takenQuantityInBaseUnit)
            movedQuantityLeft = movedQuantityLeft.subtract(movedQuantity)
            stockEntry.quantityRemaining = stockEntry.quantityRemaining.subtract(takenQuantityInBaseUnit)
            touchedEntries.add(stockEntry)
            runningBalance = runningBalance.subtract(takenQuantityInBaseUnit)
            movements.add(
                StockMovementEntity(
                    stockEntryId = stockEntry.id!!,
                    locationProductId = fifoConsumptionRequest.locationProductId,
                    movementType = fifoConsumptionRequest.movementType,
                    movedQuantity = movedQuantity,
                    remainingQuantity = runningBalance,
                    externalReferenceNumber = fifoConsumptionRequest.externalReferenceNumber,
                    unitId = fifoConsumptionRequest.unitId,
                    conversionNumerator = conversionRatio.numerator,
                    conversionDenominator = conversionRatio.denominator,
                )
            )
            unsatisfiedQuantity = unsatisfiedQuantity.subtract(takenQuantityInBaseUnit)
            consumedCost = consumedCost.add(takenQuantityInBaseUnit.multiply(stockEntry.unitCost))
        }
        throwIfNotFulfilled(fifoConsumptionRequest, unsatisfiedQuantity, productLabel)
        return FifoConsumptionResult(movements, touchedEntries, Decimals.Scale4.round(consumedCost), runningBalance)
    }

    fun restore(
        movementsToReverse: List<StockMovementEntity>,
        stockEntriesById: Map<UUID, StockEntryEntity>,
        startingBalance: BigDecimal,
        reversalType: MovementType
    ): FifoRestorationResult {
        var runningBalance = startingBalance
        val touchedEntries = mutableListOf<StockEntryEntity>()
        val reversalMovements = mutableListOf<StockMovementEntity>()
        movementsToReverse.forEach { movementToReverse ->
            val stockEntry = stockEntriesById.getValue(movementToReverse.stockEntryId)
            val conversionRatio = movementToReverse.conversionRatio()
            val restoredBaseQuantity = conversionRatio.applyTo(movementToReverse.movedQuantity)
            stockEntry.quantityRemaining = stockEntry.quantityRemaining.add(restoredBaseQuantity)
            touchedEntries.add(stockEntry)
            runningBalance = runningBalance.add(restoredBaseQuantity)
            reversalMovements.add(
                StockMovementEntity(
                    stockEntryId = movementToReverse.stockEntryId,
                    locationProductId = movementToReverse.locationProductId,
                    movementType = reversalType,
                    movedQuantity = movementToReverse.movedQuantity,
                    remainingQuantity = runningBalance,
                    externalReferenceNumber = movementToReverse.externalReferenceNumber,
                    unitId = movementToReverse.unitId,
                    conversionNumerator = conversionRatio.numerator,
                    conversionDenominator = conversionRatio.denominator,
                )
            )
        }
        return FifoRestorationResult(reversalMovements, touchedEntries, runningBalance)
    }

    private fun throwIfNotFulfilled(
        fifoConsumptionRequest: FifoConsumptionRequest,
        unsatisfiedQuantity: BigDecimal,
        productLabel: String
    ) {
        if (unsatisfiedQuantity <= BigDecimal.ZERO) return
        val formattedAvailable = Decimals.stripZeroesAndRound(fifoConsumptionRequest.baseQuantity.subtract(unsatisfiedQuantity))
        val formattedRequested = Decimals.stripZeroesAndRound(fifoConsumptionRequest.baseQuantity)
        throw RtsGenericException(
            "Insufficient stock for $productLabel. Available: $formattedAvailable, Requested: $formattedRequested"
        )
    }
}
