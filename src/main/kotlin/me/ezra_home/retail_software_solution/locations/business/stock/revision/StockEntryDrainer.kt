package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementCreator
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal

object StockEntryDrainer {

    // The cost leaves with the units, so the drain is not a write-off. A zero-quantity layer has nothing to drain and gets no movement.
    fun drain(stockEntryDrainRequest: StockEntryDrainRequest): StockMovementEntity? {
        val stockEntry = stockEntryDrainRequest.stockEntry
        val drainedQuantity = ExactQuantity.ofStorage(stockEntry.storageQuantityRemaining)
        val drainedCost = stockEntry.costRemaining
        if (!drainedQuantity.isPositive() && drainedCost.signum() == 0) return null

        stockEntry.storageQuantityRemaining = ExactQuantity.ZERO.toStorageQuantity()
        stockEntry.costRemaining = BigDecimal.ZERO

        val stockRevisionReferenceNumber = stockEntryDrainRequest.stockRevisionReferenceNumber
        val quantityBreakdown = stockEntryDrainRequest.quantityBreakdown
        val storageUnit = quantityBreakdown.storageUnitAsEntered(drainedQuantity)
        return StockMovementCreator.create(
            stockEntryId = stockEntry.id!!,
            locationProductId = stockEntry.locationProductId,
            movementType = MovementType.STOCK_REVISION_ZEROING,
            storageMoved = drainedQuantity,
            balanceAfterMovement = stockEntryDrainRequest.balanceBeforeDrain - drainedQuantity,
            movedCost = drainedCost,
            wholeLineQuantityText = storageUnit.userProvidedQuantityText,
            documentReference = stockRevisionReferenceNumber,
            detailReference = stockEntry.detailReference,
            unitId = storageUnit.unitId,
            storageUnitsPerUserProvidedUnit = storageUnit.storageUnitsPerUserProvidedUnit,
            quantityBreakdown = quantityBreakdown,
            context = StockMovementContexts.drainedByStockRevision(stockRevisionReferenceNumber, revisedSourceTextOf(stockEntry)),
            stockRevisionReference = stockRevisionReferenceNumber,
        )
    }

    private fun revisedSourceTextOf(stockEntry: StockEntryEntity): String {
        val sourceReferences = listOfNotNull(stockEntry.documentReference, stockEntry.detailReference).joinToString("/")
        return if (stockEntry.sourceType == StockItemSource.PURCHASE) "delivery $sourceReferences" else sourceReferences
    }
}
