package me.ezra_home.retail_software_solution.locations.business.stock.api.fifo

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal


object FifoTakePlanner {

    // Does not mutate the layers; the result is short of storageQuantity when stock runs out
    fun planTakes(layers: List<FifoLayer>, storageQuantity: ExactQuantity): List<PlannedTake> {
        var unsatisfiedQuantity = storageQuantity
        val plannedTakes = mutableListOf<PlannedTake>()
        for (layer in layers) {
            if (!unsatisfiedQuantity.isPositive()) break
            val layerRemaining = ExactQuantity.ofStorage(layer.storageQuantityRemaining)
            if (!layerRemaining.isPositive()) continue
            val takenQuantity = minOf(unsatisfiedQuantity, layerRemaining)
            plannedTakes.add(
                PlannedTake(layer.stockEntryId, takenQuantity, costOfTake(layer, takenQuantity, layerRemaining))
            )
            unsatisfiedQuantity -= takenQuantity
        }
        return plannedTakes
    }

    // Prices the take against the batch's exact proportion, not against what is left, so rounding never compounds across takes.
    // A void restores the exact cost it took, which can leave a layer cents off its target and make the next take negative; clamped, and the draining take recovers the difference.
    private fun costOfTake(layer: FifoLayer, takenQuantity: ExactQuantity, layerRemaining: ExactQuantity): BigDecimal {
        val remainingAfterTake = (layerRemaining - takenQuantity).toStorageQuantity()
        val costAfterTake = Decimals.MoneyScale.divide(layer.batchCost.multiply(remainingAfterTake), layer.storageBatchSize)
        return Decimals.MoneyScale.round((layer.costRemaining - costAfterTake).max(BigDecimal.ZERO))
    }
}
