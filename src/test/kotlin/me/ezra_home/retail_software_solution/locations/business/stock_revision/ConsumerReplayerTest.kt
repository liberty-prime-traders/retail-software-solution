package me.ezra_home.retail_software_solution.locations.business.stock_revision

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoLayer
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsumerReplayerTest {

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, baseTime)),
        unitId
    )
    private val revisedStockEntryId = UUID.randomUUID()

    private fun layer(reference: String, quantity: String, batchCost: String) = StockEntryFifoDto(
        referenceNumber = reference,
        locationProductId = locationProductId,
        unitCost = BigDecimal.ONE,
        fifoLayer = FifoLayer(
            stockEntryId = UUID.randomUUID(),
            storageQuantityRemaining = BigDecimal(quantity),
            costRemaining = BigDecimal(batchCost),
            storageBatchSize = BigDecimal(quantity),
            batchCost = BigDecimal(batchCost),
        ),
    )

    private fun draw(
        documentReference: String,
        detailReference: String,
        quantity: String,
        cost: String,
        stockEntryId: UUID = revisedStockEntryId,
        movementType: MovementType = MovementType.SALE,
        category: StockAdjustmentCategory? = null,
    ) = StockMovementDto(
        id = null,
        createdOn = null,
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = movementType,
        storageMovedQuantity = BigDecimal(quantity),
        movedCost = BigDecimal(cost),
        documentReference = documentReference,
        detailReference = detailReference,
        adjustmentDetails = category?.let { StockAdjustmentMovementDetails(it, null, "counted short") },
        quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line of $documentReference"),
    )

    private fun consumption(movement: StockMovementDto) = StockEntryConsumption(
        stockEntryId = movement.stockEntryId,
        locationProductId = movement.locationProductId,
        consumer = StockConsumer.of(movement),
        quantityAsEntered = movement.quantityAsEntered,
        orderOfConsumption = OrderOfConsumption.of(movement),
        netConsumedStorageQuantity = ExactQuantity.ofStorage(movement.storageMovedQuantity),
        netConsumedCost = movement.movedCost,
    )

    private fun replay(
        consumptions: List<StockEntryConsumption>,
        fifoEntriesInFifoOrder: List<StockEntryFifoDto>
    ) = ConsumerReplayer.replay(
        ReplayRequest(
            consumptionsOnReplacedLayer = consumptions,
            productBeingReplayed = ProductBeingReplayed(
                fifoEntriesAvailableForConsumption = fifoEntriesInFifoOrder,
                quantityBreakdown = quantityBreakdown,
                productName = "Sugar"
            ),
            stockRevisionReferenceNumber = "STRV000001"
        )
    )

    @Test
    fun `consumers replay oldest draw first whatever order they are listed in`() {
        val laterSale = draw("SAL1", "SALL1", "4", "40.00").copy(createdOn = baseTime.plusMinutes(5))
        val earlierSale = draw("SAL2", "SALL1", "3", "30.00").copy(createdOn = baseTime)

        val replayResult = replay(
            listOf(consumption(laterSale), consumption(earlierSale)),
            listOf(layer("STE3", "50", "700.00"))
        )

        assertEquals(
            listOf("SAL2", "SAL1"),
            replayResult.consumerReplayPlans.map { it.fifoConsumptionRequest.documentReference }
        )
    }

    @Test
    fun `each consumer replays in its original order at the replacement's cost, reporting old and new cost`() {
        val replacement = layer("STE2", "10", "120.00")
        val replayResult = replay(
            listOf(
                consumption(draw("SAL1", "SALL1", "4", "40.00")),
                consumption(draw("SAL2", "SALL1", "3", "30.00")),
            ),
            listOf(replacement, layer("STE3", "50", "700.00"))
        )

        assertTrue(replayResult.problems.isEmpty())
        assertEquals(
            listOf("SAL1", "SAL2"),
            replayResult.consumerReplayPlans.map { it.fifoConsumptionRequest.documentReference }
        )
        assertEquals(
            listOf(BigDecimal("40.00"), BigDecimal("30.00")),
            replayResult.consumerReplayPlans.map { it.oldCost }
        )
        assertEquals(
            listOf(BigDecimal("48.00"), BigDecimal("36.00")),
            replayResult.consumerReplayPlans.map { it.newCost }
        )
        val replacementId = replacement.fifoLayer.stockEntryId
        assertEquals(
            listOf(listOf(replacementId), listOf(replacementId)),
            replayResult.consumerReplayPlans.map { plan -> plan.plannedTakes.map { it.stockEntryId } }
        )
    }

    @Test
    fun `a later consumer is planned against what the earlier ones left`() {
        val replacement = layer("STE2", "5", "60.00")
        val laterLayer = layer("STE3", "50", "700.00")
        val replayResult = replay(
            listOf(
                consumption(draw("SAL1", "SALL1", "4", "40.00")),
                consumption(draw("SAL2", "SALL1", "3", "30.00")),
            ),
            listOf(replacement, laterLayer)
        )

        assertTrue(replayResult.problems.isEmpty())
        val secondPlan = replayResult.consumerReplayPlans.last()
        assertEquals(
            listOf(replacement.fifoLayer.stockEntryId, laterLayer.fifoLayer.stockEntryId),
            secondPlan.plannedTakes.map { it.stockEntryId }
        )
        assertEquals(
            listOf(BigDecimal("1.0000"), BigDecimal("2.0000")),
            secondPlan.plannedTakes.map { it.storageQuantity.toStorageQuantity() }
        )
    }

    @Test
    fun `the replayed draw keeps the draw's type, references, unit and whole-line text and names the revision`() {
        val replayResult = replay(
            listOf(consumption(draw("SAL1", "SALL1", "4", "40.00"))),
            listOf(layer("STE2", "10", "120.00"))
        )

        val fifoConsumptionRequest = replayResult.consumerReplayPlans.single().fifoConsumptionRequest
        assertEquals(MovementType.SALE, fifoConsumptionRequest.movementType)
        assertEquals("SAL1", fifoConsumptionRequest.documentReference)
        assertEquals("SALL1", fifoConsumptionRequest.detailReference)
        assertEquals(unitId, fifoConsumptionRequest.unitId)
        assertEquals(1L, fifoConsumptionRequest.storageUnitsPerUserProvidedUnit)
        assertEquals("whole line of SAL1", fifoConsumptionRequest.wholeLineQuantityText)
        assertEquals("Replayed after stock revision STRV000001", fifoConsumptionRequest.context)
    }

    @Test
    fun `an adjustment keeps its category and note`() {
        val replayResult = replay(
            listOf(
                consumption(
                    draw(
                        "ADJ1",
                        "ADJL1",
                        "2",
                        "20.00",
                        movementType = MovementType.STOCK_ADJUSTMENT_DECREASE,
                        category = StockAdjustmentCategory.INTERNAL_USE
                    )
                )
            ),
            listOf(layer("STE2", "10", "120.00"))
        )

        val adjustmentDetails = replayResult.consumerReplayPlans.single().fifoConsumptionRequest.adjustmentDetails
        assertEquals(StockAdjustmentCategory.INTERNAL_USE, adjustmentDetails?.category)
        assertEquals("counted short", adjustmentDetails?.note)
    }

    @Test
    fun `a consumer later stock cannot cover is reported and plans nothing, while the others still replay`() {
        val replayResult = replay(
            listOf(
                consumption(draw("SAL1", "SALL1", "4", "40.00")),
                consumption(draw("SAL2", "SALL1", "4", "40.00")),
            ),
            listOf(layer("STE2", "5", "60.00"))
        )

        assertEquals(
            listOf("SAL1"),
            replayResult.consumerReplayPlans.map { it.fifoConsumptionRequest.documentReference }
        )
        val problem = replayResult.problems.single()
        assertEquals("SAL2", problem.documentReference)
        assertTrue(problem.description.startsWith("Sale that took"))
        assertTrue(problem.effect.contains("could be covered"))
    }

    @Test
    fun `the total taken adds up what each consumer took from the delivery`() {
        val replayResult = replay(
            listOf(
                consumption(draw("SAL1", "SALL1", "1", "10.00").copy(createdOn = baseTime)),
                consumption(draw("SAL2", "SALL1", "4", "40.00").copy(createdOn = baseTime.plusMinutes(1))),
                consumption(draw("SAL3", "SALL1", "1", "10.00").copy(createdOn = baseTime.plusMinutes(2))),
            ),
            emptyList()
        )

        assertEquals(listOf("SAL1", "SAL2", "SAL3"), replayResult.problems.map { it.documentReference })
        assertEquals("After your change none of it could be covered.", replayResult.problems[0].effect)
        assertEquals(ExactQuantity.ofStorage(BigDecimal("6")), replayResult.totalTakenByConsumers)
    }

    @Test
    fun `a partly covered consumer takes what is left, and the total taken counts the covered ones too`() {
        val replayResult = replay(
            listOf(
                consumption(draw("SAL1", "SALL1", "4", "40.00").copy(createdOn = baseTime)),
                consumption(draw("SAL2", "SALL1", "4", "40.00").copy(createdOn = baseTime.plusMinutes(1))),
            ),
            listOf(layer("STE2", "5", "60.00"))
        )

        assertTrue(replayResult.problems.single().effect.startsWith("After your change only 1 pc"))
        assertEquals(ExactQuantity.ofStorage(BigDecimal("8")), replayResult.totalTakenByConsumers)
    }

    @Test
    fun `a replayed draw carries the revision reference so it may land on a layer the consumer already drew from`() {
        val replayResult = replay(
            listOf(consumption(draw("SAL1", "SALL1", "4", "40.00"))),
            listOf(layer("STE3", "50", "700.00"))
        )

        assertTrue(replayResult.problems.isEmpty())
        assertEquals(
            "STRV000001",
            replayResult.consumerReplayPlans.single().fifoConsumptionRequest.stockRevisionReference
        )
    }

    @Test
    fun `consumptions of two layers are rejected because a revision replaces one`() {
        assertThrows(IllegalArgumentException::class.java) {
            replay(
                listOf(
                    consumption(draw("SAL1", "SALL1", "4", "40.00", stockEntryId = UUID.randomUUID())),
                    consumption(draw("SAL2", "SALL1", "3", "30.00", stockEntryId = UUID.randomUUID())),
                ),
                listOf(layer("STE2", "10", "120.00"))
            )
        }
    }

    @Test
    fun `no consumptions replay nothing`() {
        val replayResult = replay(emptyList(), listOf(layer("STE2", "10", "120.00")))

        assertTrue(replayResult.consumerReplayPlans.isEmpty())
        assertTrue(replayResult.problems.isEmpty())
    }
}
