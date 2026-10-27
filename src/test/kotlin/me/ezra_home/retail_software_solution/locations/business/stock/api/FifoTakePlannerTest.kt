package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class FifoTakePlannerTest {

    private val thousandPieceBatchSize = BigDecimal("1000")
    private val threeUnitBatchCost = BigDecimal("3.00")

    @Test
    fun `selling a sub-cent layer one piece at a time books the exact proportion to within a cent at every step`() {
        var layer = thousandPieceLayer()
        var bookedCost = BigDecimal.ZERO
        for (piecesSold in 1..1000) {
            val plannedTake = FifoTakePlanner.planTakes(listOf(layer), ExactQuantity.ofStorage(BigDecimal.ONE)).single()
            bookedCost += plannedTake.movedCost
            layer = layer.copy(
                storageQuantityRemaining = layer.storageQuantityRemaining - BigDecimal.ONE,
                costRemaining = layer.costRemaining - plannedTake.movedCost
            )
            val exactCost = threeUnitBatchCost.multiply(BigDecimal(piecesSold)).divide(thousandPieceBatchSize)
            assertTrue((bookedCost - exactCost).abs() <= BigDecimal("0.005"), "after $piecesSold pieces booked $bookedCost against exact $exactCost")
        }

        assertEquals(0, threeUnitBatchCost.compareTo(bookedCost))
    }

    @Test
    fun `half of a sub-cent layer books half of its cost`() {
        val plannedTake = FifoTakePlanner.planTakes(listOf(thousandPieceLayer()), ExactQuantity.ofStorage(BigDecimal("500"))).single()

        assertEquals(BigDecimal("1.50"), plannedTake.movedCost)
    }

    @Test
    fun `a layer left a cent below its target after an out of order void never prices a take below zero`() {
        val layerAfterVoid = thousandPieceLayer().copy(
            storageQuantityRemaining = BigDecimal("999"),
            costRemaining = BigDecimal("2.99")
        )

        val plannedTake = FifoTakePlanner.planTakes(listOf(layerAfterVoid), ExactQuantity.ofStorage(BigDecimal.ONE)).single()

        assertEquals(BigDecimal("0.00"), plannedTake.movedCost)
    }

    @Test
    fun `a clamped take followed by the draining take still sums to exactly what the layer held`() {
        val layerAfterVoid = thousandPieceLayer().copy(
            storageQuantityRemaining = BigDecimal("999"),
            costRemaining = BigDecimal("2.99")
        )
        val clampedTake = FifoTakePlanner.planTakes(listOf(layerAfterVoid), ExactQuantity.ofStorage(BigDecimal.ONE)).single()
        val layerAfterClampedTake = layerAfterVoid.copy(
            storageQuantityRemaining = BigDecimal("998"),
            costRemaining = layerAfterVoid.costRemaining - clampedTake.movedCost
        )

        val drainingTake = FifoTakePlanner.planTakes(listOf(layerAfterClampedTake), ExactQuantity.ofStorage(BigDecimal("998"))).single()

        assertEquals(0, BigDecimal.ZERO.compareTo(clampedTake.movedCost))
        assertEquals(0, layerAfterVoid.costRemaining.compareTo(clampedTake.movedCost + drainingTake.movedCost))
    }

    @Test
    fun `after takes and out of order voids in any mix, draining the layer still books exactly the batch cost and no take is negative`() {
        val random = java.util.Random(42)
        repeat(200) {
            var layer = thousandPieceLayer()
            val takes = mutableListOf<PlannedTake>()
            repeat(300) {
                val shouldVoid = takes.isNotEmpty() && random.nextInt(3) == 0
                if (shouldVoid) {
                    val voided = takes.removeAt(random.nextInt(takes.size))
                    layer = layer.copy(
                        storageQuantityRemaining = layer.storageQuantityRemaining + voided.storageQuantity.toStorageQuantity(),
                        costRemaining = layer.costRemaining + voided.movedCost
                    )
                } else if (layer.storageQuantityRemaining.signum() > 0) {
                    val pieces = BigDecimal(1 + random.nextInt(minOf(5, layer.storageQuantityRemaining.toInt())))
                    val remainingAfterTake = layer.storageQuantityRemaining - pieces
                    val plannedTake = FifoTakePlanner.planTakes(listOf(layer), ExactQuantity.ofStorage(pieces)).single()
                    assertTrue(plannedTake.movedCost.signum() >= 0)
                    takes.add(plannedTake)
                    layer = layer.copy(
                        storageQuantityRemaining = remainingAfterTake,
                        costRemaining = layer.costRemaining - plannedTake.movedCost
                    )
                }
            }
            val drainingTake = FifoTakePlanner.planTakes(listOf(layer), ExactQuantity.ofStorage(layer.storageQuantityRemaining)).single()
            assertEquals(0, layer.costRemaining.compareTo(drainingTake.movedCost))
            val netBooked = takes.fold(drainingTake.movedCost) { sum, take -> sum + take.movedCost }
            assertEquals(0, threeUnitBatchCost.compareTo(netBooked))
        }
    }

    @Test
    fun `the take that empties a layer costs everything left in it`() {
        val layer = thousandPieceLayer().copy(storageQuantityRemaining = BigDecimal("3"), costRemaining = BigDecimal("0.02"))

        val plannedTake = FifoTakePlanner.planTakes(listOf(layer), ExactQuantity.ofStorage(BigDecimal("3"))).single()

        assertEquals(BigDecimal("0.02"), plannedTake.movedCost)
    }

    private fun thousandPieceLayer() = StockEntryFifoDto(
        locationProductId = UUID.randomUUID(),
        unitCost = threeUnitBatchCost,
        storageQuantityRemaining = thousandPieceBatchSize,
        costRemaining = threeUnitBatchCost,
        storageBatchSize = thousandPieceBatchSize,
        batchCost = threeUnitBatchCost
    )
}
