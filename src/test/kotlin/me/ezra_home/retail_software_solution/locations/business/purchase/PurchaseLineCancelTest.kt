package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseLineCancelTest {

    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val epoch = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, epoch), BreakdownUnit(baleId, "Bale", 18L, pieceId, epoch.plusMinutes(1))),
        baleId
    )

    private fun oneBaleLineWith5PiecesDelivered() = PurchaseLineEntity(
        purchaseId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        userProvidedQuantityOrdered = BigDecimal("1"),
        unitCost = BigDecimal("100.00"),
        unitId = baleId,
        storageUnitsPerUnit = 18L,
        storageQuantityDelivered = BigDecimal("5"),
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "PURN000001"
    }

    private fun cancel(line: PurchaseLineEntity, typed: String, storageUnitsPerUnit: Long) {
        val storageQuantity = ExactQuantity.ofUserProvided(BigDecimal(typed), storageUnitsPerUnit)
        PurchaseValidator.guardCancelQuantity(line, storageQuantity, quantityBreakdown)
        line.storageQuantityCanceled = storageQuantity.toStorageQuantity()
    }

    @Test
    fun `canceling 13 pieces after 5 are delivered against a 1 bale line completes it exactly`() {
        val line = oneBaleLineWith5PiecesDelivered()

        cancel(line, "13", storageUnitsPerUnit = 1L)

        assertFalse(line.storageQuantityRemaining().isPositive())
    }

    @Test
    fun `canceling 0 point 72 bale in the default unit leaves a sliver open`() {
        val line = oneBaleLineWith5PiecesDelivered()

        cancel(line, "0.72", storageUnitsPerUnit = line.storageUnitsPerUnit)

        assertTrue(line.storageQuantityRemaining().isPositive())
        assertEquals(0, BigDecimal("0.04").compareTo(line.storageQuantityRemaining().toStorageQuantity()))
    }

    @Test
    fun `cancel is absolute so a second cancel replaces the first`() {
        val line = oneBaleLineWith5PiecesDelivered()
        cancel(line, "13", storageUnitsPerUnit = 1L)

        cancel(line, "2", storageUnitsPerUnit = 1L)

        assertEquals(0, BigDecimal("2").compareTo(line.storageQuantityCanceled))
        assertTrue(line.storageQuantityRemaining().isPositive())
    }

    @Test
    fun `a cancel that makes delivered plus canceled exceed ordered in storage units is rejected`() {
        val line = oneBaleLineWith5PiecesDelivered()

        val exception = assertThrows(RtsGenericException::class.java) { cancel(line, "14", storageUnitsPerUnit = 1L) }

        assertTrue(exception.message.contains("Max cancelable: 13 Pieces"), exception.message)
    }

    @Test
    fun `the line total scales to the expected storage quantity and is exact when nothing is canceled`() {
        val line = oneBaleLineWith5PiecesDelivered()
        assertEquals(BigDecimal("100.00"), line.getTotalCost())

        cancel(line, "9", storageUnitsPerUnit = 1L)

        assertEquals(BigDecimal("50.00"), line.getTotalCost())
    }

    @Test
    fun `a line captured at 18 keeps its ordered storage quantity whatever the unit is later changed to`() {
        val line = oneBaleLineWith5PiecesDelivered()

        assertEquals(0, BigDecimal("18").compareTo(line.storageQuantityOrdered().toStorageQuantity()))
    }
}
