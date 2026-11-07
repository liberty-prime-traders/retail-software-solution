package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class PlannedTake(val stockEntryId: UUID, val storageQuantity: ExactQuantity, val movedCost: BigDecimal)

data class StockEntryFifoDto(
    val referenceNumber: String,
    val locationProductId: UUID,
    val unitCost: BigDecimal,
    val fifoLayer: FifoLayer,
) {

    fun afterTake(plannedTake: PlannedTake): StockEntryFifoDto = copy(fifoLayer = fifoLayer.afterTake(plannedTake))
}

data class FifoLayer(
    val stockEntryId: UUID,
    val storageQuantityRemaining: BigDecimal,
    val costRemaining: BigDecimal,
    val storageBatchSize: BigDecimal,
    val batchCost: BigDecimal,
) {

    fun afterTake(plannedTake: PlannedTake): FifoLayer = copy(
        storageQuantityRemaining = (ExactQuantity.ofStorage(storageQuantityRemaining) - plannedTake.storageQuantity).toStorageQuantity(),
        costRemaining = costRemaining.subtract(plannedTake.movedCost)
    )
}
