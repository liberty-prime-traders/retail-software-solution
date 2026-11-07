package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementDraft
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal

data class StockEntryRestoration(
    val reversalDrafts: List<StockMovementDraft>,
    val endingBalance: ExactQuantity,
    val restoredQuantity: ExactQuantity,
    val restoredCost: BigDecimal,
)

object StockEntryRestorer {

    fun restore(
        stockEntryConsumptions: List<StockEntryConsumption>,
        stockEntryDto: StockEntryDto,
        stockRevisionReferenceNumber: String,
        startingBalance: ExactQuantity,
    ): StockEntryRestoration {
        rejectConsumptionsThatCannotBeRestored(stockEntryConsumptions, stockEntryDto)
        var runningBalance = startingBalance
        val reversalDrafts = stockEntryConsumptions.map { stockEntryConsumption ->
            runningBalance += stockEntryConsumption.netConsumedStorageQuantity
            reversalDraftOf(stockEntryConsumption, stockRevisionReferenceNumber, runningBalance)
        }
        return StockEntryRestoration(
            reversalDrafts = reversalDrafts,
            endingBalance = runningBalance,
            restoredQuantity = runningBalance - startingBalance,
            restoredCost = stockEntryConsumptions.sumOf { it.netConsumedCost },
        )
    }

    private fun reversalDraftOf(
        stockEntryConsumption: StockEntryConsumption,
        stockRevisionReferenceNumber: String,
        balanceAfterMovement: ExactQuantity
    ): StockMovementDraft {
        val quantityAsEntered = stockEntryConsumption.quantityAsEntered
        return StockMovementDraft(
            movementType = MovementType.REVERSED_BY_REVISION,
            storageMoved = stockEntryConsumption.netConsumedStorageQuantity,
            balanceAfterMovement = balanceAfterMovement,
            movedCost = stockEntryConsumption.netConsumedCost,
            wholeLineQuantityText = quantityAsEntered.userProvidedQuantityText,
            documentReference = stockEntryConsumption.stockMovementOrigin.documentReference,
            detailReference = stockEntryConsumption.stockMovementOrigin.detailReference,
            unitId = quantityAsEntered.unitId,
            storageUnitsPerUserProvidedUnit = quantityAsEntered.storageUnitsPerUserProvidedUnit,
            context = StockMovementContexts.restoredForStockRevision(stockRevisionReferenceNumber),
            stockRevisionReference = stockRevisionReferenceNumber,
        )
    }

    private fun rejectConsumptionsThatCannotBeRestored(
        stockEntryConsumptions: List<StockEntryConsumption>,
        stockEntryDto: StockEntryDto
    ) {
        stockEntryConsumptions
            .filter { it.consumer.movementType in MovementType.movementsThatCannotBeReversed }
            .map { entryConsumption ->
                "Stock entry ${stockEntryDto.referenceNumber}: " +
                    "cannot restore ${entryConsumption.stockMovementOrigin.displayText} because " +
                    "${entryConsumption.consumer.movementType} cannot be reversed."
            }
            .let { errors -> if (errors.isNotEmpty()) throw RtsGenericException("Consumptions cannot be restored", payload = errors) }
    }
}
