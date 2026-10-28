package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockMovementCreatorTest {

    private val pieceId = UUID.randomUUID()
    private val baleId = UUID.randomUUID()
    private val epoch = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val baleBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, epoch), BreakdownUnit(baleId, "Bale", 18L, pieceId, epoch.plusMinutes(1))),
        baleId
    )
    private val stockEntryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()

    private fun createMovement(adjustmentDetails: StockAdjustmentMovementDetails? = null) = StockMovementCreator.create(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = MovementType.SALE,
        storageMoved = ExactQuantity.ofStorage(BigDecimal("-20")),
        balanceAfterMovement = ExactQuantity.ofStorage(BigDecimal("16")),
        movedCost = BigDecimal("12.3400"),
        wholeLineQuantityText = "1.1111 Bales",
        documentReference = "SAL000001",
        detailReference = "SALN000001",
        unitId = baleId,
        storageUnitsPerUserProvidedUnit = 18L,
        quantityBreakdown = baleBreakdown,
        adjustmentDetails = adjustmentDetails,
    )

    @Test
    fun `the movement copies the entry, product, type, references, unit ratio and moved cost it is given`() {
        val movement = createMovement()

        assertEquals(stockEntryId, movement.stockEntryId)
        assertEquals(locationProductId, movement.locationProductId)
        assertEquals(MovementType.SALE, movement.movementType)
        assertEquals("SAL000001", movement.documentReference)
        assertEquals("SALN000001", movement.detailReference)
        assertEquals(baleId, movement.unitId)
        assertEquals(18L, movement.storageUnitsPerUserProvidedUnit)
        assertEquals(BigDecimal("12.3400"), movement.movedCost)
    }

    @Test
    fun `the moved and remaining quantities are stored signed in storage units`() {
        val movement = createMovement()

        assertEquals(BigDecimal("-20.0000"), movement.storageMovedQuantity)
        assertEquals(BigDecimal("16.0000"), movement.storageRemainingQuantity)
    }

    @Test
    fun `the whole line text is kept as given while both storage texts are rendered from the breakdown`() {
        val movement = createMovement()

        assertEquals("1.1111 Bales", movement.wholeLineQuantityText)
        assertEquals("-1 Bale 2 Pieces", movement.storageMovedBreakdownText)
        assertEquals("16 Pieces", movement.storageRemainingBreakdownText)
    }

    @Test
    fun `a movement without adjustment details has no reason, category or note`() {
        val movement = createMovement()

        assertNull(movement.reasonId)
        assertNull(movement.category)
        assertNull(movement.note)
    }

    @Test
    fun `adjustment details supply the reason, category and note`() {
        val reasonId = UUID.randomUUID()

        val movement = createMovement(StockAdjustmentMovementDetails(StockAdjustmentCategory.FREE_GOODS, reasonId, "sample"))

        assertEquals(reasonId, movement.reasonId)
        assertEquals(StockAdjustmentCategory.FREE_GOODS, movement.category)
        assertEquals("sample", movement.note)
    }
}
