package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.locations.business.stock.api.FifoTakePlanner
import me.ezra_home.retail_software_solution.locations.business.stock.api.PlannedTake
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFifoDto
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object StockTransferFifoAllocator {

    // Cost groups are runs, not a group-by, so consuming them in order walks the layers in the order they were planned
    fun allocateDispatchLines(
        dispatchId: UUID,
        draftLines: List<StockTransferDraftLineEntity>,
        fifoEntriesByProduct: Map<UUID, List<StockEntryFifoDto>>,
        orgProductIdByLocationProductId: Map<UUID, UUID>,
        quantityBreakdownByLocationProductId: Map<UUID, QuantityBreakdown>,
        productLabelByLocationProductId: Map<UUID, String>
    ): List<StockTransferDispatchLineEntity> {
        return draftLines.flatMap { draftLine ->
            val storageQuantityNeeded = ExactQuantity.ofUserProvided(draftLine.userProvidedQuantity, draftLine.storageUnitsPerUnit)
            val fifoEntries = fifoEntriesByProduct[draftLine.locationProductId].orEmpty()
            val plannedTakes = FifoTakePlanner.planTakes(fifoEntries, storageQuantityNeeded)
            guardFulfilled(
                plannedTakes, storageQuantityNeeded,
                quantityBreakdownByLocationProductId.getValue(draftLine.locationProductId),
                productLabelByLocationProductId.getValue(draftLine.locationProductId)
            )
            groupIntoCostRuns(plannedTakes, fifoEntries).map { costRun ->
                StockTransferDispatchLineEntity(
                    stockTransferDispatchId = dispatchId,
                    orgProductId = orgProductIdByLocationProductId.getValue(draftLine.locationProductId),
                    locationProductId = draftLine.locationProductId,
                    wholeLineUserProvidedQuantity = draftLine.userProvidedQuantity,
                    storageQuantityDispatched = costRun.storageQuantity.toStorageQuantity(),
                    unitId = draftLine.unitId,
                    unitCost = Decimals.Scale10.divide(costRun.cost, costRun.storageQuantity.toStorageQuantity()),
                    costDispatched = costRun.cost,
                    storageUnitsPerUnit = draftLine.storageUnitsPerUnit
                )
            }
        }
    }

    private fun groupIntoCostRuns(plannedTakes: List<PlannedTake>, fifoEntries: List<StockEntryFifoDto>): List<CostRun> {
        val costRuns = mutableListOf<CostRun>()
        var currentRate: BigDecimal? = null
        plannedTakes.forEach { plannedTake ->
            val rate = Decimals.Scale4.round(fifoEntries[plannedTake.layerIndex].unitCost)
            val currentRun = costRuns.lastOrNull()
            if (currentRun != null && rate.compareTo(currentRate) == 0) {
                costRuns[costRuns.lastIndex] = CostRun(currentRun.storageQuantity + plannedTake.storageQuantity, currentRun.cost.add(plannedTake.movedCost))
            } else {
                costRuns.add(CostRun(plannedTake.storageQuantity, plannedTake.movedCost))
                currentRate = rate
            }
        }
        return costRuns
    }

    private fun guardFulfilled(
        plannedTakes: List<PlannedTake>,
        storageQuantityNeeded: ExactQuantity,
        quantityBreakdown: QuantityBreakdown,
        productLabel: String
    ) {
        val available = plannedTakes.fold(ExactQuantity.ZERO) { sum, plannedTake -> sum + plannedTake.storageQuantity }
        if (available >= storageQuantityNeeded) return
        throw RtsGenericException(
            "Insufficient stock for $productLabel. Available: ${quantityBreakdown.render(available)}, " +
                "Requested: ${quantityBreakdown.render(storageQuantityNeeded)}"
        )
    }
}

private data class CostRun(val storageQuantity: ExactQuantity, val cost: BigDecimal)
