package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.util.UUID

object StockEntryRestorer {

    fun restore(
        entryConsumptions: List<EntryConsumption>,
        stockEntriesById: Map<UUID, StockEntryEntity>,
        stockRevisionReferenceNumber: String,
        runningBalanceByLocationProductId: MutableMap<UUID, ExactQuantity>,
        quantityBreakdownByLocationProductId: Map<UUID, QuantityBreakdown>
    ): List<StockMovementEntity> {
        rejectConsumptionsThatCannotBeRestored(entryConsumptions, stockEntriesById)
        return entryConsumptions.map { entryConsumption ->
            restoreConsumption(
                entryConsumption,
                stockEntriesById.getValue(entryConsumption.stockEntryId),
                stockRevisionReferenceNumber,
                runningBalanceByLocationProductId,
                quantityBreakdownByLocationProductId
            )
        }
    }

    private fun restoreConsumption(
        entryConsumption: EntryConsumption,
        stockEntry: StockEntryEntity,
        stockRevisionReferenceNumber: String,
        runningBalanceByLocationProductId: MutableMap<UUID, ExactQuantity>,
        quantityBreakdownByLocationProductId: Map<UUID, QuantityBreakdown>
    ): StockMovementEntity {
        val consumingMovement = entryConsumption.consumingMovement
        val locationProductId = consumingMovement.locationProductId
        stockEntry.storageQuantityRemaining =
            (ExactQuantity.ofStorage(stockEntry.storageQuantityRemaining) + entryConsumption.netConsumedStorageQuantity).toStorageQuantity()
        stockEntry.costRemaining = stockEntry.costRemaining.add(entryConsumption.netConsumedCost)
        val balanceAfterMovement = runningBalanceByLocationProductId.getValue(locationProductId) + entryConsumption.netConsumedStorageQuantity
        runningBalanceByLocationProductId[locationProductId] = balanceAfterMovement
        return StockMovementCreator.create(
            stockEntryId = stockEntry.id!!,
            locationProductId = locationProductId,
            movementType = MovementType.REVERSED_BY_REVISION,
            storageMoved = entryConsumption.netConsumedStorageQuantity,
            balanceAfterMovement = balanceAfterMovement,
            movedCost = entryConsumption.netConsumedCost,
            wholeLineQuantityText = consumingMovement.wholeLineQuantityText,
            documentReference = entryConsumption.documentReference,
            detailReference = entryConsumption.detailReference,
            unitId = consumingMovement.unitId,
            storageUnitsPerUserProvidedUnit = consumingMovement.storageUnitsPerUserProvidedUnit,
            quantityBreakdown = quantityBreakdownByLocationProductId.getValue(locationProductId),
            context = StockMovementContexts.restoredForStockRevision(stockRevisionReferenceNumber),
        )
    }

    private fun rejectConsumptionsThatCannotBeRestored(
        entryConsumptions: List<EntryConsumption>,
        stockEntriesById: Map<UUID, StockEntryEntity>
    ) {
        entryConsumptions
            .filter { it.consumingMovement.movementType in MovementType.movementsThatCannotBeReversed }
            .map { entryConsumption ->
                "Stock entry ${stockEntriesById.getValue(entryConsumption.stockEntryId).requiredReference()}: " +
                    "cannot restore ${entryConsumption.documentAndDetailReference} because " +
                    "${entryConsumption.consumingMovement.movementType} cannot be reversed."
            }
            .let { errors -> if (errors.isNotEmpty()) throw RtsGenericException("Consumptions cannot be restored", payload = errors) }
    }
}
