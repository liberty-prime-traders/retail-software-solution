package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.FifoTakePlanner
import me.ezra_home.retail_software_solution.locations.business.stock.api.PlannedTake
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal

// Replays the consumptions of one replaced layer, each as one draw of its net quantity. A consumer that also drew from other
// layers keeps that part as it was, so a revision never has more than one batch to put back.
// Plans on copies of the layers and writes nothing, so a refused replay leaves every layer as it was.
// A later consumer is planned against what the earlier ones left, which is why the order below matters.
object ConsumerReplayer {

    fun replay(replayRequest: ReplayRequest): ReplayResult {
        val consumptionsOnReplacedLayer = replayRequest.consumptionsOnReplacedLayer
        if (consumptionsOnReplacedLayer.isEmpty()) return ReplayResult(emptyList(), emptyList())
        StockEntryConsumption.requireOnOneLayer(consumptionsOnReplacedLayer)
        val productBeingReplayed = replayRequest.productBeingReplayed
        var availableFifoEntries = productBeingReplayed.fifoEntriesAvailableForConsumption
        val errors = mutableListOf<String>()
        val consumerReplayPlans = mutableListOf<ConsumerReplayPlan>()

        consumptionsOnReplacedLayer.sortedBy { it.orderOfConsumption }.forEach { stockEntryConsumption ->
            val availableStockLayers = availableFifoEntries.map { it.fifoLayer }
            val plannedTakes = FifoTakePlanner.planTakes(availableStockLayers, stockEntryConsumption.netConsumedStorageQuantity)

            uncoveredQuantityErrorOf(stockEntryConsumption, plannedTakes, productBeingReplayed)
                ?.let {
                    errors.add(it)
                    return@forEach
                }

            availableFifoEntries = afterTakes(availableFifoEntries, plannedTakes)
            consumerReplayPlans.add(
                ConsumerReplayPlan(
                    fifoConsumptionRequest = fifoConsumptionRequestOf(
                        stockEntryConsumption, productBeingReplayed, replayRequest.stockRevisionReferenceNumber
                    ),
                    plannedTakes = plannedTakes,
                    oldCost = stockEntryConsumption.netConsumedCost,
                )
            )
        }
        return ReplayResult(consumerReplayPlans, errors)
    }

    private fun uncoveredQuantityErrorOf(
        stockEntryConsumption: StockEntryConsumption,
        plannedTakes: List<PlannedTake>,
        productBeingReplayed: ProductBeingReplayed
    ): String? {
        val quantityRequired = stockEntryConsumption.netConsumedStorageQuantity
        val availableQuantity = plannedTakes.fold(ExactQuantity.ZERO) { sum, plannedTake -> sum + plannedTake.storageQuantity }
        if (availableQuantity >= quantityRequired) return null
        val quantityBreakdown = productBeingReplayed.quantityBreakdown
        return "Cannot replay ${stockEntryConsumption.stockMovementOrigin.displayText}" +
                " for ${productBeingReplayed.productLabel}:" +
                " later stock covers ${quantityBreakdown.render(availableQuantity)}" +
                " of ${quantityBreakdown.render(quantityRequired)}."
    }

    private fun afterTakes(fifoEntries: List<StockEntryFifoDto>, plannedTakes: List<PlannedTake>): List<StockEntryFifoDto> {
        val plannedTakeByStockEntryId = plannedTakes.associateBy { it.stockEntryId }
        return fifoEntries.map { fifoEntry ->
            val plannedTake = plannedTakeByStockEntryId[fifoEntry.fifoLayer.stockEntryId]
            if (plannedTake == null) fifoEntry else fifoEntry.afterTake(plannedTake)
        }
    }

    private fun fifoConsumptionRequestOf(
        stockEntryConsumption: StockEntryConsumption,
        productBeingReplayed: ProductBeingReplayed,
        stockRevisionReferenceNumber: String
    ): FifoConsumptionRequest {
        val consumer = stockEntryConsumption.consumer
        val quantityAsEntered = stockEntryConsumption.quantityAsEntered
        val replayedQuantity = stockEntryConsumption.netConsumedStorageQuantity
        val stockMovementOrigin = stockEntryConsumption.stockMovementOrigin
        return FifoConsumptionRequest(
            locationProductId = stockEntryConsumption.locationProductId,
            storageQuantity = replayedQuantity,
            userProvidedQuantity = Decimals.Scale10.divide(
                replayedQuantity.toStorageQuantity(),
                BigDecimal(quantityAsEntered.storageUnitsPerUserProvidedUnit)
            ),
            unitId = quantityAsEntered.unitId,
            storageUnitsPerUserProvidedUnit = quantityAsEntered.storageUnitsPerUserProvidedUnit,
            movementType = consumer.movementType,
            documentReference = stockMovementOrigin.documentReference
                ?: throw RtsGenericException("A draw for ${productBeingReplayed.productLabel} has no document reference and cannot be replayed."),
            detailReference = stockMovementOrigin.detailReference,
            adjustmentDetails = consumer.adjustmentDetails,
            wholeLineQuantityText = quantityAsEntered.userProvidedQuantityText,
            context = StockMovementContexts.replayedAfterStockRevision(stockRevisionReferenceNumber),
            stockRevisionReference = stockRevisionReferenceNumber,
        )
    }
}
