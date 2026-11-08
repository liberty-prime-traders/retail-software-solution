package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoLayer
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.AdjustmentCostChange
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionPreviewSeverity
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class RevisionPreviewStoryWriterTest {

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, baseTime)),
        unitId
    )
    private val revisedStockEntryId = UUID.randomUUID()

    private fun layer(quantity: String, batchCost: String) = StockEntryFifoDto(
        referenceNumber = "STE1",
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

    private fun consumption(
        documentReference: String,
        quantity: String,
        cost: String,
        movementType: MovementType = MovementType.SALE
    ): StockEntryConsumption {
        val movement = StockMovementDto(
            id = null,
            createdOn = null,
            stockEntryId = revisedStockEntryId,
            locationProductId = locationProductId,
            movementType = movementType,
            storageMovedQuantity = BigDecimal(quantity),
            movedCost = BigDecimal(cost),
            documentReference = documentReference,
            detailReference = "LINE1",
            adjustmentDetails = null,
            quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line of $documentReference"),
        )
        return StockEntryConsumption(
            stockEntryId = movement.stockEntryId,
            locationProductId = movement.locationProductId,
            consumer = StockConsumer.of(movement),
            quantityAsEntered = movement.quantityAsEntered,
            orderOfConsumption = OrderOfConsumption.of(movement),
            netConsumedStorageQuantity = ExactQuantity.ofStorage(movement.storageMovedQuantity),
            netConsumedCost = movement.movedCost,
        )
    }

    private fun replay(consumptions: List<StockEntryConsumption>, layers: List<StockEntryFifoDto>) =
        ConsumerReplayer.replay(
            ReplayRequest(
                stockRevisionReferenceNumber = "PREVIEW",
                consumptionsOnReplacedLayer = consumptions,
                productBeingReplayed = ProductBeingReplayed("Sugar", quantityBreakdown, layers)
            )
        )

    @Test
    fun `a sale whose cost rises reports the profit it loses, a transfer reports none`() {
        val replayResult = replay(
            listOf(
                consumption("SAL1", "4", "40.00"),
                consumption("TRF1", "3", "30.00", MovementType.TRANSFER_OUT)
            ),
            listOf(layer("10", "120.00"))
        )

        val items = RevisionPreviewStoryWriter.write(replayResult, emptyList(), quantityBreakdown, null, emptyMap())

        assertEquals(listOf("SAL1/LINE1", "TRF1/LINE1"), items.map { it.documentReference })
        assertEquals(RevisionPreviewSeverity.NOTE, items.first().severity)
        assertEquals(0, BigDecimal("-8.00").compareTo(items.first().netProfitChange))
        assertTrue(items.first().description.startsWith("Sale that took"))
        assertTrue(items.last().description.startsWith("Transfer that took"))
        assertNull(items.last().netProfitChange)
    }

    @Test
    fun `a consumer priced wholly from the replacement says so`() {
        val replacement = layer("10", "120.00")
        val replayResult = replay(listOf(consumption("SAL1", "4", "40.00")), listOf(replacement))

        val item = RevisionPreviewStoryWriter.write(
            replayResult, emptyList(), quantityBreakdown, replacement.fifoLayer.stockEntryId, emptyMap()
        ).single()

        assertTrue(item.effect.endsWith("All of it is priced at this delivery's revised cost."))
    }

    @Test
    fun `a consumer the replacement only partly covers says how the rest is priced`() {
        val replacement = layer("3", "60.00")
        val laterDelivery = layer("50", "700.00")
        val replayResult = replay(listOf(consumption("SAL1", "4", "40.00")), listOf(replacement, laterDelivery))

        val item = RevisionPreviewStoryWriter.write(
            replayResult, emptyList(), quantityBreakdown, replacement.fifoLayer.stockEntryId, emptyMap()
        ).single()

        assertTrue(item.effect.contains("This delivery covers only 3 pcs"))
        assertTrue(item.effect.endsWith("is priced from other deliveries at their own cost."))
        assertTrue(item.effect.contains("of it at its revised cost; the other 1 pc"))
    }

    @Test
    fun `a consumer the replacement does not cover at all says so`() {
        val laterDelivery = layer("50", "700.00")
        val replayResult = replay(listOf(consumption("SAL1", "4", "40.00")), listOf(laterDelivery))

        val item = RevisionPreviewStoryWriter.write(
            replayResult, emptyList(), quantityBreakdown, UUID.randomUUID(), emptyMap()
        ).single()

        assertTrue(
            item.effect.endsWith(
                "This delivery has none of it left, so all of it is priced from other deliveries at their own cost."
            )
        )
    }

    @Test
    fun `an adjustment reports the profit its restated cost moves, and one with no known accounts reports none`() {
        val replayResult = replay(
            listOf(
                consumption("ADJ1", "4", "40.00", MovementType.STOCK_ADJUSTMENT_DECREASE),
                consumption("ADJ2", "3", "30.00", MovementType.STOCK_ADJUSTMENT_DECREASE),
            ),
            listOf(layer("10", "120.00"))
        )
        val adjustmentCostChanges = mapOf(
            "ADJ1" to AdjustmentCostChange(
                SystemAccount.INTERNAL_USE_EXPENSE.code, SystemAccount.INVENTORY.code, BigDecimal("8.00")
            )
        )

        val items = RevisionPreviewStoryWriter.write(
            replayResult, emptyList(), quantityBreakdown, null, adjustmentCostChanges
        )

        val internalUseItem = items.first { it.documentReference == "ADJ1/LINE1" }
        assertEquals(0, BigDecimal("-8.00").compareTo(internalUseItem.netProfitChange))
        assertNull(items.first { it.documentReference == "ADJ2/LINE1" }.netProfitChange)
    }

    @Test
    fun `a consumer whose cost does not move is left out`() {
        val replayResult = replay(listOf(consumption("SAL1", "4", "40.00")), listOf(layer("10", "100.00")))

        assertTrue(
            RevisionPreviewStoryWriter.write(replayResult, emptyList(), quantityBreakdown, null, emptyMap()).isEmpty()
        )
    }

    @Test
    fun `while a consumer blocks the save only blocking rows are shown, not the cost rows beside them`() {
        val replayResult = replay(
            listOf(consumption("SAL1", "4", "40.00"), consumption("SAL2", "9", "90.00")),
            listOf(layer("6", "60.00"))
        )

        val items = RevisionPreviewStoryWriter.write(replayResult, emptyList(), quantityBreakdown, null, emptyMap())

        assertEquals(1, items.size)
        assertEquals(RevisionPreviewSeverity.BLOCKS_SAVE, items.single().severity)
        assertEquals("SAL2", items.single().documentReference)
        assertTrue(items.single().effect.contains("could be covered"))
    }

    @Test
    fun `a short reservation is a blocking row listed ahead of the cost rows`() {
        val replayResult = replay(listOf(consumption("SAL1", "4", "40.00")), listOf(layer("10", "120.00")))
        val reservationProblem = BlockingProblem(
            "SAL000420 (draft)", "Draft sale holding 4 pcs of Sugar.", "Remove it."
        )

        val items = RevisionPreviewStoryWriter.write(
            replayResult, listOf(reservationProblem), quantityBreakdown, null, emptyMap()
        )

        assertEquals(listOf("SAL000420 (draft)", "SAL1/LINE1"), items.map { it.documentReference })
        assertEquals(
            listOf(RevisionPreviewSeverity.BLOCKS_SAVE, RevisionPreviewSeverity.NOTE),
            items.map { it.severity }
        )
    }

    @Test
    fun `the change summary is one sentence`() {
        val summary = RevisionPreviewStoryWriter.changeSummary(
            ValidatedDeliveryLineRevisionDto(
                revisedDeliveryLineId = UUID.randomUUID(),
                deliveryReferenceNumber = "DEL1",
                revisedLineReferenceNumber = "DLL1",
                purchaseLineId = UUID.randomUUID(),
                locationProductId = locationProductId,
                revisedUnitId = unitId,
                revisedStorageUnitsPerUserProvidedUnit = 1L,
                unitId = unitId,
                storageUnitsPerUserProvidedUnit = 1L,
                revisedUserProvidedQuantityDelivered = BigDecimal("200"),
                revisedUnitCost = BigDecimal("17"),
                userProvidedQuantityDelivered = BigDecimal("180"),
                unitCost = BigDecimal("18"),
                problems = emptyList(),
            ),
            quantityBreakdown
        )

        assertTrue(summary.contains(" becomes "))
        assertTrue(summary.endsWith("."))
    }
}
