package me.ezra_home.retail_software_solution.locations.business.stock_revision

import java.math.BigDecimal
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoTakePlanner
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.PlannedTake
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity

// A later consumer is planned against what the earlier ones left, so the order below matters.
object ConsumerReplayer {

    fun replay(replayRequest: ReplayRequest): ReplayResult {
        val consumptionsOnReplacedLayer = replayRequest.consumptionsOnReplacedLayer
        if (consumptionsOnReplacedLayer.isEmpty()) return ReplayResult(emptyList(), emptyList())
        StockEntryConsumption.requireOnOneLayer(consumptionsOnReplacedLayer)
        val productBeingReplayed = replayRequest.productBeingReplayed
        var availableFifoEntries = productBeingReplayed.fifoEntriesAvailableForConsumption
        val problems = mutableListOf<BlockingProblem>()
        var totalTakenByUncoveredConsumers = ExactQuantity.ZERO
        val movementTypesOfUncoveredConsumers = mutableSetOf<MovementType>()
        val consumerReplayPlans = mutableListOf<ConsumerReplayPlan>()

        consumptionsOnReplacedLayer.sortedBy { it.orderOfConsumption }.forEach { stockEntryConsumption ->
            val availableStockLayers = availableFifoEntries.map { it.fifoLayer }
            val plannedTakes = FifoTakePlanner.planTakes(
                availableStockLayers,
                stockEntryConsumption.netConsumedStorageQuantity
            )

            val coveredQuantity = plannedTakes.fold(ExactQuantity.ZERO) { sum, plannedTake ->
                sum + plannedTake.storageQuantity
            }
            availableFifoEntries = afterTakes(availableFifoEntries, plannedTakes)
            val uncoveredQuantity = stockEntryConsumption.netConsumedStorageQuantity - coveredQuantity
            if (uncoveredQuantity > ExactQuantity.ZERO) {
                totalTakenByUncoveredConsumers += stockEntryConsumption.netConsumedStorageQuantity
                movementTypesOfUncoveredConsumers.add(stockEntryConsumption.consumer.movementType)
                problems.add(
                    uncoveredQuantityProblemOf(stockEntryConsumption, coveredQuantity, productBeingReplayed)
                )
                return@forEach
            }
            consumerReplayPlans.add(
                ConsumerReplayPlan(
                    fifoConsumptionRequest = fifoConsumptionRequestOf(
                        stockEntryConsumption,
                        replayRequest.stockRevisionReferenceNumber
                    ),
                    plannedTakes = plannedTakes,
                    oldCost = stockEntryConsumption.netConsumedCost,
                )
            )
        }
        return ReplayResult(consumerReplayPlans, problems, totalTakenByUncoveredConsumers, movementTypesOfUncoveredConsumers)
    }

    private fun uncoveredQuantityProblemOf(
        stockEntryConsumption: StockEntryConsumption,
        coveredQuantity: ExactQuantity,
        productBeingReplayed: ProductBeingReplayed
    ): BlockingProblem {
        val quantityBreakdown = productBeingReplayed.quantityBreakdown
        val coveredText = if (coveredQuantity > ExactQuantity.ZERO) {
            "only ${quantityBreakdown.render(coveredQuantity)} of it"
        } else {
            "none of it"
        }
        return BlockingProblem(
            documentReference = stockEntryConsumption.stockMovementOrigin.requiredDocumentReference(),
            description = "${StockConsumerLabel.of(stockEntryConsumption.consumer.movementType)} that took " +
                "${quantityBreakdown.render(stockEntryConsumption.netConsumedStorageQuantity)} of " +
                "${productBeingReplayed.productName} from this delivery.",
            effect = "After your change $coveredText could be covered."
        )
    }

    private fun afterTakes(
        fifoEntries: List<StockEntryFifoDto>,
        plannedTakes: List<PlannedTake>
    ): List<StockEntryFifoDto> {
        val plannedTakeByStockEntryId = plannedTakes.associateBy { it.stockEntryId }
        return fifoEntries.map { fifoEntry ->
            val plannedTake = plannedTakeByStockEntryId[fifoEntry.fifoLayer.stockEntryId]
            if (plannedTake == null) fifoEntry else fifoEntry.afterTake(plannedTake)
        }
    }

    private fun fifoConsumptionRequestOf(
        stockEntryConsumption: StockEntryConsumption,
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
            documentReference = stockMovementOrigin.requiredDocumentReference(),
            detailReference = stockMovementOrigin.detailReference,
            adjustmentDetails = consumer.adjustmentDetails,
            wholeLineQuantityText = quantityAsEntered.userProvidedQuantityText,
            context = StockMovementContexts.replayedAfterStockRevision(stockRevisionReferenceNumber),
            stockRevisionReference = stockRevisionReferenceNumber,
        )
    }
}
