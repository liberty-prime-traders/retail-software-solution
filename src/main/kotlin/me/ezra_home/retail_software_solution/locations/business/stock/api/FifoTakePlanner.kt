package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal

data class FifoLayerState(val storageQuantityRemaining: BigDecimal, val costRemaining: BigDecimal)

data class PlannedTake(val layerIndex: Int, val storageQuantity: ExactQuantity, val movedCost: BigDecimal)

object FifoTakePlanner {

    // Does not mutate the layers; the result is short of storageQuantity when stock runs out
    fun planTakes(layers: List<FifoLayerState>, storageQuantity: ExactQuantity): List<PlannedTake> {
        var unsatisfiedQuantity = storageQuantity
        val plannedTakes = mutableListOf<PlannedTake>()
        for ((layerIndex, layer) in layers.withIndex()) {
            if (!unsatisfiedQuantity.isPositive()) break
            val layerRemaining = ExactQuantity.ofStorage(layer.storageQuantityRemaining)
            if (!layerRemaining.isPositive()) continue
            val takenQuantity = minOf(unsatisfiedQuantity, layerRemaining)
            plannedTakes.add(PlannedTake(layerIndex, takenQuantity, costOfTake(layer.costRemaining, takenQuantity, layerRemaining)))
            unsatisfiedQuantity -= takenQuantity
        }
        return plannedTakes
    }

    // The take that empties a layer returns all its remaining cost, so a layer's takes sum exactly to its batch cost
    private fun costOfTake(costRemaining: BigDecimal, takenQuantity: ExactQuantity, layerRemaining: ExactQuantity): BigDecimal =
        if (takenQuantity == layerRemaining) {
            costRemaining
        } else {
            Decimals.Scale4.divide(
                costRemaining.multiply(takenQuantity.toStorageQuantity()),
                layerRemaining.toStorageQuantity()
            )
        }
}
