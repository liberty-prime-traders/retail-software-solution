package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal

interface FifoLayer {
    val storageQuantityRemaining: BigDecimal
    val costRemaining: BigDecimal
    val storageBatchSize: BigDecimal
    val batchCost: BigDecimal
}

data class PlannedTake(val layerIndex: Int, val storageQuantity: ExactQuantity, val movedCost: BigDecimal)

object FifoTakePlanner {

    // Does not mutate the layers; the result is short of storageQuantity when stock runs out
    fun planTakes(layers: List<FifoLayer>, storageQuantity: ExactQuantity): List<PlannedTake> {
        var unsatisfiedQuantity = storageQuantity
        val plannedTakes = mutableListOf<PlannedTake>()
        for ((layerIndex, layer) in layers.withIndex()) {
            if (!unsatisfiedQuantity.isPositive()) break
            val layerRemaining = ExactQuantity.ofStorage(layer.storageQuantityRemaining)
            if (!layerRemaining.isPositive()) continue
            val takenQuantity = minOf(unsatisfiedQuantity, layerRemaining)
            plannedTakes.add(PlannedTake(layerIndex, takenQuantity, costOfTake(layer, takenQuantity, layerRemaining)))
            unsatisfiedQuantity -= takenQuantity
        }
        return plannedTakes
    }

    // Prices the take against the batch's exact proportion, not against what is left, so rounding never compounds across takes.
    // A void restores the exact cost it took, which can leave a layer a cent off its target and make the next take negative; clamped.
    private fun costOfTake(layer: FifoLayer, takenQuantity: ExactQuantity, layerRemaining: ExactQuantity): BigDecimal {
        val remainingAfterTake = (layerRemaining - takenQuantity).toStorageQuantity()
        val costAfterTake = Decimals.MoneyScale.divide(layer.batchCost.multiply(remainingAfterTake), layer.storageBatchSize)
        return Decimals.MoneyScale.round((layer.costRemaining - costAfterTake).max(BigDecimal.ZERO))
    }
}
