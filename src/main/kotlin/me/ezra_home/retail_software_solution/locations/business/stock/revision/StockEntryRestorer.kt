package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementCreator
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown

data class StockEntryRestoration(
    val reversalMovements: List<StockMovementEntity>,
    val endingBalance: ExactQuantity,
)

object StockEntryRestorer {

    fun restore(
        stockEntryConsumptions: List<StockEntryConsumption>,
        stockEntry: StockEntryEntity,
        stockRevisionReferenceNumber: String,
        startingBalance: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): StockEntryRestoration {
        rejectConsumptionsThatCannotBeRestored(stockEntryConsumptions, stockEntry)
        var runningBalance = startingBalance
        val reversalMovements = stockEntryConsumptions.map { stockEntryConsumption ->
            runningBalance += stockEntryConsumption.netConsumedStorageQuantity
            restoreConsumption(stockEntryConsumption, stockEntry, stockRevisionReferenceNumber, runningBalance, quantityBreakdown)
        }
        return StockEntryRestoration(reversalMovements, runningBalance)
    }

    private fun restoreConsumption(
        stockEntryConsumption: StockEntryConsumption,
        stockEntry: StockEntryEntity,
        stockRevisionReferenceNumber: String,
        balanceAfterMovement: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): StockMovementEntity {
        val quantityAsEntered = stockEntryConsumption.quantityAsEntered
        val currentStockBalance = ExactQuantity.ofStorage(stockEntry.storageQuantityRemaining)
        val stockBalanceWhenFull = currentStockBalance + stockEntryConsumption.netConsumedStorageQuantity
        stockEntry.storageQuantityRemaining = stockBalanceWhenFull.toStorageQuantity()
        stockEntry.costRemaining = stockEntry.costRemaining.add(stockEntryConsumption.netConsumedCost)
        return StockMovementCreator.create(
            stockEntryId = stockEntry.id!!,
            locationProductId = stockEntryConsumption.locationProductId,
            movementType = MovementType.REVERSED_BY_REVISION,
            storageMoved = stockEntryConsumption.netConsumedStorageQuantity,
            balanceAfterMovement = balanceAfterMovement,
            movedCost = stockEntryConsumption.netConsumedCost,
            wholeLineQuantityText = quantityAsEntered.userProvidedQuantityText,
            documentReference = stockEntryConsumption.stockMovementOrigin.documentReference,
            detailReference = stockEntryConsumption.stockMovementOrigin.detailReference,
            unitId = quantityAsEntered.unitId,
            storageUnitsPerUserProvidedUnit = quantityAsEntered.storageUnitsPerUserProvidedUnit,
            quantityBreakdown = quantityBreakdown,
            context = StockMovementContexts.restoredForStockRevision(stockRevisionReferenceNumber),
            stockRevisionReference = stockRevisionReferenceNumber,
        )
    }

    private fun rejectConsumptionsThatCannotBeRestored(
        stockEntryConsumptions: List<StockEntryConsumption>,
        stockEntry: StockEntryEntity
    ) {
        stockEntryConsumptions
            .filter { it.consumer.movementType in MovementType.movementsThatCannotBeReversed }
            .map { entryConsumption ->
                "Stock entry ${stockEntry.requiredReference()}: " +
                    "cannot restore ${entryConsumption.stockMovementOrigin.displayText} because " +
                    "${entryConsumption.consumer.movementType} cannot be reversed."
            }
            .let { errors -> if (errors.isNotEmpty()) throw RtsGenericException("Consumptions cannot be restored", payload = errors) }
    }
}
