package me.ezra_home.retail_software_solution.locations.business.stock.api.fifo

import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class FifoLayerTest {

    private fun layer(quantity: String, cost: String) = FifoLayer(
        stockEntryId = UUID.randomUUID(),
        storageQuantityRemaining = BigDecimal(quantity),
        costRemaining = BigDecimal(cost),
        storageBatchSize = BigDecimal(quantity),
        batchCost = BigDecimal(cost),
    )

    private fun take(layers: List<FifoLayer>, quantity: String) = FifoTakePlanner.planTakes(
        layers,
        ExactQuantity.ofStorage(BigDecimal(quantity))
    )

    @Test
    fun `afterTake returns the layer with the taken quantity and cost removed and leaves the original alone`() {
        val original = layer("10", "100.00")

        val advanced = original.afterTake(take(listOf(original), "4").single())

        assertEquals(BigDecimal("6.0000"), advanced.storageQuantityRemaining)
        assertEquals(BigDecimal("60.00"), advanced.costRemaining)
        assertEquals(BigDecimal("10"), original.storageQuantityRemaining)
        assertEquals(BigDecimal("100.00"), original.costRemaining)
    }

    @Test
    fun `an entry advanced by a take keeps its identity and only its layer changes`() {
        val original = StockEntryFifoDto("STE1", UUID.randomUUID(), BigDecimal.ONE, layer("10", "100.00"))

        val advanced = original.afterTake(take(listOf(original.fifoLayer), "4").single())

        assertEquals(original.fifoLayer.stockEntryId, advanced.fifoLayer.stockEntryId)
        assertEquals("STE1", advanced.referenceNumber)
        assertEquals(BigDecimal("6.0000"), advanced.fifoLayer.storageQuantityRemaining)
        assertEquals(BigDecimal("10"), original.fifoLayer.storageQuantityRemaining)
    }

    @Test
    fun `takes planned one after another on advanced layers drain the layers in FIFO order`() {
        var layers = listOf(layer("5", "50.00"), layer("10", "200.00"))

        val firstTake = take(layers, "4")
        val layerIds = layers.map { it.stockEntryId }
        layers = layers.map { layer -> firstTake.find { it.stockEntryId == layer.stockEntryId }?.let(
            layer::afterTake
        ) ?: layer }
        val secondTake = take(layers, "4")

        assertEquals(listOf(layerIds[0]), firstTake.map { it.stockEntryId })
        assertEquals(layerIds, secondTake.map { it.stockEntryId })
        assertEquals(
            listOf(BigDecimal("1.0000"), BigDecimal("3.0000")),
            secondTake.map { it.storageQuantity.toStorageQuantity() }
        )
    }
}
