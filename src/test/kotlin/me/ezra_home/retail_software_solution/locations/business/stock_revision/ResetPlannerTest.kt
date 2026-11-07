package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class ResetPlannerTest {

    private val stockMovementFetcher = mock(StockMovementFetcher::class.java)
    private val stockEntryFetcher = mock(StockEntryFetcher::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val netConsumptionCalculator = NetConsumptionCalculator(stockMovementFetcher, stockEntryFetcher)
    private val resetPlanner = ResetPlanner(stockEntryFetcher, stockBalanceFetcher)

    private val stockEntryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")
    private val stockRevisionReferenceNumber = "STRV000001"
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, baseTime)), unitId)

    private fun stockEntry(remaining: String, costRemaining: String, status: StockEntryStatus = StockEntryStatus.ACTIVE) = StockEntryDto(
        id = stockEntryId,
        referenceNumber = "STE000001",
        locationProductId = locationProductId,
        sourceType = StockItemSource.PURCHASE,
        documentReference = "DLV1",
        detailReference = "DLL1",
        storageQuantityRemaining = BigDecimal(remaining),
        costRemaining = BigDecimal(costRemaining),
        status = status,
    )

    private fun movement(
        movementType: MovementType,
        quantity: String,
        cost: String,
        documentReference: String,
        minutesAfterBase: Long = 0,
        movementUnitId: UUID = unitId,
        movementStorageUnitsPerUserProvidedUnit: Long = 1L
    ) = StockMovementDto(
        id = null,
        createdOn = baseTime.plusMinutes(minutesAfterBase),
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = movementType,
        storageMovedQuantity = BigDecimal(quantity),
        movedCost = BigDecimal(cost),
        documentReference = documentReference,
        detailReference = "LINE1",
        adjustmentDetails = null,
        quantityAsEntered = QuantityAsEntered(movementUnitId, movementStorageUnitsPerUserProvidedUnit, "text"),
    )

    private fun received() = movement(MovementType.PURCHASE_RECEIVED, "10", "100.00", "DLV1")

    private fun plan(stockEntry: StockEntryDto, movementsOnEntry: List<StockMovementDto>, productBalance: String) {
        val movementDtos = movementsOnEntry
        `when`(stockMovementFetcher.findOnEntry(stockEntryId)).thenReturn(movementDtos)
        `when`(stockMovementFetcher.findOfDocuments(any(UUID::class.java) ?: locationProductId, anySet<String>() ?: emptySet())).thenReturn(movementDtos)
        `when`(stockEntryFetcher.get(stockEntryId)).thenReturn(stockEntry)
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(mapOf(locationProductId to BigDecimal(productBalance)))
        resetPlan = resetPlanner.plan(
            stockEntryId, netConsumptionCalculator.calculateNetConsumptions(stockEntryId), stockRevisionReferenceNumber, quantityBreakdown
        )
    }

    private lateinit var resetPlan: me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryResetPlan

    private val drafts get() = resetPlan.movementDrafts

    private fun assertNumber(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")

    private fun assertQuantity(expected: String, actual: ExactQuantity) = assertNumber(expected, actual.toStorageQuantity())

    @Test
    fun `a consumed entry is restored by reversing its draw and then drained to zero`() {
        plan(stockEntry("6", "60.00"), listOf(received(), movement(MovementType.SALE, "4", "40.00", "SALE1", 1)), productBalance = "6")

        assertEquals(listOf(MovementType.REVERSED_BY_REVISION, MovementType.STOCK_REVISION_ZEROING), drafts.map { it.movementType })
        val reversal = drafts[0]
        assertQuantity("4", reversal.storageMoved)
        assertNumber("40.00", reversal.movedCost)
        assertEquals("SALE1", reversal.documentReference)
        assertEquals("LINE1", reversal.detailReference)
        assertQuantity("10", reversal.balanceAfterMovement)
        assertEquals(StockMovementContexts.restoredForStockRevision(stockRevisionReferenceNumber), reversal.context)
        assertEquals(stockRevisionReferenceNumber, reversal.stockRevisionReference)
        val drain = drafts[1]
        assertQuantity("10", drain.storageMoved)
        assertNumber("100.00", drain.movedCost)
        assertEquals(stockRevisionReferenceNumber, drain.documentReference)
        assertEquals("DLL1", drain.detailReference)
        assertEquals(stockRevisionReferenceNumber, drain.stockRevisionReference)
        assertEquals("Drained by stock revision STRV000001, which revised delivery DLV1/DLL1", drain.context)
        assertQuantity("0", drain.balanceAfterMovement)
        assertEquals(stockEntryId, resetPlan.stockEntryId)
        assertEquals(locationProductId, resetPlan.locationProductId)
    }

    @Test
    fun `an entry nobody drew from is only drained`() {
        plan(stockEntry("10", "100.00"), listOf(received()), productBalance = "10")

        assertEquals(listOf(MovementType.STOCK_REVISION_ZEROING), drafts.map { it.movementType })
        assertNumber("100.00", drafts.single().movedCost)
    }

    @Test
    fun `a zero-quantity entry plans no drain movement`() {
        plan(stockEntry("0", "0.00"), listOf(received()), productBalance = "0")

        assertTrue(drafts.isEmpty())
    }

    @Test
    fun `a transfer is restored with a reversed by revision movement`() {
        plan(stockEntry("6", "60.00"), listOf(received(), movement(MovementType.TRANSFER_OUT, "4", "40.00", "TRF1", 1)), productBalance = "6")

        assertEquals(MovementType.REVERSED_BY_REVISION, drafts.first().movementType)
    }

    @Test
    fun `an adjustment is restored with a reversed by revision movement`() {
        plan(stockEntry("7", "70.00"), listOf(received(), movement(MovementType.STOCK_ADJUSTMENT_DECREASE, "3", "30.00", "ADJ1", 1)), productBalance = "7")

        assertEquals(MovementType.REVERSED_BY_REVISION, drafts.first().movementType)
    }

    @Test
    fun `a consumer that cannot be reversed is rejected`() {
        val exception = assertThrows(RtsGenericException::class.java) {
            plan(stockEntry("7", "70.00"), listOf(received(), movement(MovementType.SUPPLIER_RETURN, "3", "30.00", "SRT1", 1)), productBalance = "7")
        }

        assertEquals(1, (exception.payload as List<*>).size)
    }

    @Test
    fun `a partly reversed draw restores only what is still out`() {
        plan(
            stockEntry("7", "70.00"),
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", 1),
                movement(MovementType.SALE_VOID, "1", "10.00", "SALE1", 2)
            ),
            productBalance = "7"
        )

        assertEquals(MovementType.REVERSED_BY_REVISION, drafts.first().movementType)
        assertQuantity("3", drafts.first().storageMoved)
        assertNumber("30.00", drafts.first().movedCost)
    }

    @Test
    fun `an entry that was already replaced is rejected`() {
        val exception = assertThrows(RtsGenericException::class.java) {
            plan(stockEntry("0", "0.00", StockEntryStatus.REPLACED), listOf(received()), productBalance = "0")
        }

        assertTrue((exception.payload as List<*>).single().toString().contains("was already replaced"))
    }

    @Test
    fun `the drain is recorded in the storage unit, not in the unit the layer was received in`() {
        val receivedInBoxes = movement(
            MovementType.PURCHASE_RECEIVED, "10", "100.00", "DLV1",
            movementUnitId = UUID.randomUUID(), movementStorageUnitsPerUserProvidedUnit = 12L
        )

        plan(stockEntry("10", "100.00"), listOf(receivedInBoxes), productBalance = "10")

        assertEquals(unitId, drafts.single().unitId)
        assertEquals(1L, drafts.single().storageUnitsPerUserProvidedUnit)
    }
}
