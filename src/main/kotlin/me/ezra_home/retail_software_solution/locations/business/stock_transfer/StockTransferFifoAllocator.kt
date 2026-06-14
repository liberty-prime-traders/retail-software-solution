package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFifoDto
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID


object StockTransferFifoAllocator {

    fun allocateDispatchLines(
        dispatchId: UUID,
        draftLines: List<StockTransferDraftLineEntity>,
        fifoEntriesByProduct: Map<UUID, List<StockEntryFifoDto>>,
        productIdByLocationProductId: Map<UUID, UUID>,
        productLabelByLocationProductId: Map<UUID, String>
    ): List<StockTransferDispatchLineEntity> {
        return draftLines.flatMap { draftLine ->
            val baseQtyNeeded = Decimals.multiplyScale4(draftLine.quantity, draftLine.conversionFactor)
            val fifoEntries = fifoEntriesByProduct[draftLine.locationProductId].orEmpty()
            val productLabel = productLabelByLocationProductId[draftLine.locationProductId]
                ?: draftLine.locationProductId.toString()
            allocateByCostGroup(draftLine.locationProductId, fifoEntries, baseQtyNeeded, productLabel).map { allocation ->
                StockTransferDispatchLineEntity(
                    stockTransferDispatchId = dispatchId,
                    productId = productIdByLocationProductId.getValue(draftLine.locationProductId),
                    locationProductId = draftLine.locationProductId,
                    quantityDispatched = Decimals.divideScale4(allocation.baseQuantity, draftLine.conversionFactor),
                    unitId = draftLine.unitId,
                    unitCost = allocation.unitCost,
                    conversionFactor = draftLine.conversionFactor,
                    baseUnitId = draftLine.baseUnitId
                )
            }
        }
    }

    private fun allocateByCostGroup(
        locationProductId: UUID,
        fifoEntries: List<StockEntryFifoDto>,
        baseQtyNeeded: BigDecimal,
        productLabel: String
    ): List<CostGroupAllocation> {
        val costGroups = fifoEntries
            .groupBy { it.unitCost ?: BigDecimal.ZERO }
            .map { (cost, entries) -> CostGroupAllocation(cost, entries.sumOf { it.quantityRemaining }) }

        var remaining = baseQtyNeeded
        val allocations = mutableListOf<CostGroupAllocation>()
        for (costGroup in costGroups) {
            if (remaining <= BigDecimal.ZERO) break
            val taken = remaining.min(costGroup.baseQuantity)
            allocations.add(CostGroupAllocation(unitCost = costGroup.unitCost, baseQuantity = taken))
            remaining = remaining.subtract(taken)
        }
        if (remaining > BigDecimal.ZERO) {
            val available = Decimals.stripZeroesAndRound(baseQtyNeeded.subtract(remaining))
            val requested = Decimals.stripZeroesAndRound(baseQtyNeeded)
            throw RtsGenericException(
                "Insufficient stock for $productLabel. Available: $available, Requested: $requested"
            )
        }
        return allocations
    }
}

private data class CostGroupAllocation(val unitCost: BigDecimal, val baseQuantity: BigDecimal)
