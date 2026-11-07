package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementDraft
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal

object StockEntryDrainer {

    // The cost leaves with the units, so the drain is not a write-off. A zero-quantity layer has nothing to drain and gets no movement.
    fun drain(
        stockEntryDto: StockEntryDto,
        restoredQuantity: ExactQuantity,
        restoredCost: BigDecimal,
        stockRevisionReferenceNumber: String,
        balanceBeforeDrain: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): StockMovementDraft? {
        val drainedQuantity = ExactQuantity.ofStorage(stockEntryDto.storageQuantityRemaining) + restoredQuantity
        val drainedCost = stockEntryDto.costRemaining.add(restoredCost)
        if (!drainedQuantity.isPositive() && drainedCost.signum() == 0) return null

        val storageUnit = quantityBreakdown.storageUnitAsEntered(drainedQuantity)
        return StockMovementDraft(
            movementType = MovementType.STOCK_REVISION_ZEROING,
            storageMoved = drainedQuantity,
            balanceAfterMovement = balanceBeforeDrain - drainedQuantity,
            movedCost = drainedCost,
            wholeLineQuantityText = storageUnit.userProvidedQuantityText,
            documentReference = stockRevisionReferenceNumber,
            detailReference = stockEntryDto.detailReference,
            unitId = storageUnit.unitId,
            storageUnitsPerUserProvidedUnit = storageUnit.storageUnitsPerUserProvidedUnit,
            context = StockMovementContexts.drainedByStockRevision(stockRevisionReferenceNumber, revisedSourceTextOf(stockEntryDto)),
            stockRevisionReference = stockRevisionReferenceNumber,
        )
    }

    private fun revisedSourceTextOf(stockEntryDto: StockEntryDto): String {
        val sourceReferences = listOfNotNull(stockEntryDto.documentReference, stockEntryDto.detailReference).joinToString("/")
        return if (stockEntryDto.sourceType == StockItemSource.PURCHASE) "delivery $sourceReferences" else sourceReferences
    }
}
