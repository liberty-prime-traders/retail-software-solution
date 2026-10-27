package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockInboundLayersTest {

    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val epoch = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val baleBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, epoch), BreakdownUnit(baleId, "Bale", 18L, pieceId, epoch.plusMinutes(1))),
        baleId
    )

    private fun spec(userProvidedQuantity: String, storageUnitsPerUserProvidedUnit: Long, batchCost: String) = InboundLayerSpec(
        locationProductId = UUID.randomUUID(),
        sourceType = StockItemSource.PURCHASE,
        movementType = MovementType.PURCHASE_RECEIVED,
        documentReference = "DEL000001",
        detailReference = "DELN000001",
        storageQuantity = ExactQuantity.ofStorageFromUserProvided(PositiveUserProvidedQuantity.of(BigDecimal(userProvidedQuantity)), storageUnitsPerUserProvidedUnit),
        batchCost = BigDecimal(batchCost),
        userProvidedQuantity = BigDecimal(userProvidedQuantity),
        unitId = baleId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
    )

    @Test
    fun `a layer's batch cost is the source line total and its rate is derived at scale 10`() {
        val layer = StockInboundLayers.newLayer(spec("1", 18L, "100.0000"))

        assertEquals(BigDecimal("100.0000"), layer.batchCost)
        assertEquals(BigDecimal("100.0000"), layer.costRemaining)
        assertEquals(BigDecimal("5.5555555556"), layer.unitCost)
        assertEquals(0, BigDecimal("18").compareTo(layer.storageBatchSize))
    }

    @Test
    fun `a zero cost layer does not divide by zero and a free layer has a zero rate`() {
        val layer = StockInboundLayers.newLayer(spec("1", 18L, "0.0000"))

        assertEquals(0, BigDecimal.ZERO.compareTo(layer.unitCost))
    }

    @Test
    fun `the inbound movement stores the typed text, the captured ratio and both breakdown texts`() {
        val spec = spec("2", 18L, "200.0000")
        val layer = StockInboundLayers.newLayer(spec).also { it.id = UUID.randomUUID() }

        val movement = StockInboundLayers.inboundMovement(spec, layer, ExactQuantity.ofStorage(BigDecimal("44")), baleBreakdown)

        assertEquals("2 Bales", movement.storageMovedBreakdownText)
        assertEquals("2 Bales 8 Pieces", movement.storageRemainingBreakdownText)
        assertEquals("2 Bales", movement.wholeLineQuantityText)
        assertEquals(18L, movement.storageUnitsPerUserProvidedUnit)
        assertEquals("DELN000001", movement.detailReference)
        assertEquals(BigDecimal("200.0000"), movement.movedCost)
    }
}
