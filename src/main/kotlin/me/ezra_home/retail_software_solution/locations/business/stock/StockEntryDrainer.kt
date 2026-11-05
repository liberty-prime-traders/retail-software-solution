package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object StockEntryDrainer {

    fun rejectReplacedEntries(stockEntries: Collection<StockEntryEntity>) {
        stockEntries
            .filter { it.status == StockEntryStatus.REPLACED }
            .map { "Stock entry ${it.requiredReference()} was already replaced." }
            .let { errors -> if (errors.isNotEmpty())
                throw RtsGenericException("Replaced entries cannot be drained again", payload = errors)
            }
    }

    fun drain(
        stockEntries: Collection<StockEntryEntity>,
        movementsOnEntries: List<StockMovementEntity>,
        stockRevisionReferenceNumber: String,
        runningBalanceByLocationProductId: MutableMap<UUID, ExactQuantity>,
        quantityBreakdownByLocationProductId: Map<UUID, QuantityBreakdown>
    ): List<StockMovementEntity> {
        val firstMovementByStockEntryId = movementsOnEntries
            .groupBy { it.stockEntryId }
            .mapValues { (_, movementsOnEntry) -> movementsOnEntry.first() }
        return stockEntries.sortedWith(stockEntryFifoComparator).mapNotNull { stockEntry ->
            stockEntry.status = StockEntryStatus.REPLACED
            drainEntry(
                stockEntry,
                firstMovementByStockEntryId.getValue(stockEntry.id!!),
                stockRevisionReferenceNumber,
                runningBalanceByLocationProductId,
                quantityBreakdownByLocationProductId
            )
        }
    }

    // The cost leaves with the units, so the drain is not a write-off.
    private fun drainEntry(
        stockEntry: StockEntryEntity,
        firstMovementOnEntry: StockMovementEntity,
        stockRevisionReferenceNumber: String,
        runningBalanceByLocationProductId: MutableMap<UUID, ExactQuantity>,
        quantityBreakdownByLocationProductId: Map<UUID, QuantityBreakdown>
    ): StockMovementEntity? {
        val drainedQuantity = ExactQuantity.ofStorage(stockEntry.storageQuantityRemaining)
        val drainedCost = stockEntry.costRemaining
        if (!drainedQuantity.isPositive() && drainedCost.signum() == 0) return null
        val locationProductId = stockEntry.locationProductId
        val quantityBreakdown = quantityBreakdownByLocationProductId.getValue(locationProductId)
        stockEntry.storageQuantityRemaining = ExactQuantity.ZERO.toStorageQuantity()
        stockEntry.costRemaining = BigDecimal.ZERO
        val balanceAfterMovement = runningBalanceByLocationProductId.getValue(locationProductId) - drainedQuantity
        runningBalanceByLocationProductId[locationProductId] = balanceAfterMovement
        return StockMovementCreator.create(
            stockEntryId = stockEntry.id!!,
            locationProductId = locationProductId,
            movementType = MovementType.STOCK_REVISION_ZEROING,
            storageMoved = drainedQuantity,
            balanceAfterMovement = balanceAfterMovement,
            movedCost = drainedCost,
            wholeLineQuantityText = quantityBreakdown.render(drainedQuantity),
            documentReference = stockEntry.documentReference,
            detailReference = stockEntry.detailReference,
            unitId = firstMovementOnEntry.unitId,
            storageUnitsPerUserProvidedUnit = firstMovementOnEntry.storageUnitsPerUserProvidedUnit,
            quantityBreakdown = quantityBreakdown,
            context = StockMovementContexts.drainedByStockRevision(stockRevisionReferenceNumber),
        )
    }
}
