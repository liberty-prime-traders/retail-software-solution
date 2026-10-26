package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFifoDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class StockTransferDispatchLineEntityTest {

    private val locationProductId = UUID.randomUUID()
    private val orgProductId = UUID.randomUUID()
    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val epoch = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, epoch), BreakdownUnit(baleId, "Bale", 18L, pieceId, epoch.plusMinutes(1))),
        baleId
    )

    private fun draftLine(userProvidedQuantity: String, storageUnitsPerUnit: Long) = StockTransferDraftLineEntity(
        stockTransferDispatchId = UUID.randomUUID(),
        locationProductId = locationProductId,
        userProvidedQuantity = BigDecimal(userProvidedQuantity),
        unitId = baleId,
        storageUnitsPerUnit = storageUnitsPerUnit,
    )

    private fun layer(storageQuantity: String, batchCost: String) = StockEntryFifoDto(
        locationProductId = locationProductId,
        unitCost = BigDecimal(batchCost).divide(BigDecimal(storageQuantity), 10, RoundingMode.HALF_UP),
        storageQuantityRemaining = BigDecimal(storageQuantity),
        costRemaining = BigDecimal(batchCost),
        storageBatchSize = BigDecimal(storageQuantity),
        batchCost = BigDecimal(batchCost),
    )

    private fun allocate(draft: StockTransferDraftLineEntity, layers: List<StockEntryFifoDto>) =
        StockTransferFifoAllocator.allocateDispatchLines(
            dispatchId = UUID.randomUUID(),
            draftLines = listOf(draft),
            fifoEntriesByProduct = mapOf(locationProductId to layers),
            orgProductIdByLocationProductId = mapOf(locationProductId to orgProductId),
            quantityBreakdownByLocationProductId = mapOf(locationProductId to quantityBreakdown),
            productLabelByLocationProductId = mapOf(locationProductId to "Bella"),
        )

    @Test
    fun `a dispatch across cost groups gives group costs that sum to the cost consumed and repeats the whole line figure on every row`() {
        val dispatchLines = allocate(
            draftLine("2", 18L),
            listOf(layer("10", "10.0000"), layer("20", "40.0000"), layer("30", "90.0000"))
        )

        assertEquals(3, dispatchLines.size)
        assertEquals(listOf(BigDecimal("10.0000"), BigDecimal("20.0000"), BigDecimal("6.0000")), dispatchLines.map { it.storageQuantityDispatched })
        assertEquals(BigDecimal("36.0000").compareTo(dispatchLines.sumOf { it.storageQuantityDispatched }), 0)
        assertEquals(setOf(BigDecimal("2")), dispatchLines.map { it.wholeLineUserProvidedQuantity }.toSet())
        assertEquals(setOf(18L), dispatchLines.map { it.storageUnitsPerUnit }.toSet())
        assertEquals(0, BigDecimal("10.0000").add(BigDecimal("40.0000")).add(BigDecimal("18.0000")).compareTo(dispatchLines.sumOf { it.costDispatched }))
    }

    @Test
    fun `consecutive layers at the same rate form one cost group and the same rate after a different one starts a new group`() {
        val dispatchLines = allocate(
            draftLine("1", 18L),
            listOf(layer("5", "10.00"), layer("5", "10.00"), layer("5", "20.00"), layer("20", "40.00"))
        )

        assertEquals(listOf(BigDecimal("10.0000"), BigDecimal("5.0000"), BigDecimal("3.0000")), dispatchLines.map { it.storageQuantityDispatched })
        assertEquals(listOf(BigDecimal("20.00"), BigDecimal("20.00"), BigDecimal("6.00")), dispatchLines.map { it.costDispatched })
    }

    @Test
    fun `a transfer needing more than the layers hold is rejected with breakdown figures`() {
        val exception = assertThrows(RtsGenericException::class.java) {
            allocate(draftLine("2", 18L), listOf(layer("35", "70.0000")))
        }

        assertEquals("Insufficient stock for Bella. Available: 1 Bale 17 Pieces, Requested: 2 Bales", exception.message)
    }
}
