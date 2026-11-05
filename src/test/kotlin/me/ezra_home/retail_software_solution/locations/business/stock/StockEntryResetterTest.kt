package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockEntryResetterTest {

    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockBreakdownProvider = mock(StockBreakdownProvider::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockEntryResetter = StockEntryResetter(
        entityAdvisoryLock,
        StockEntryConsumptionFinder(stockMovementRepository, stockEntryRepository),
        stockEntryRepository,
        stockMovementRepository,
        stockBalanceFetcher,
        stockBreakdownProvider
    )

    private val stockEntryId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")
    private val stockRevisionReferenceNumber = "STRV000001"
    private val savedMovements = mutableListOf<StockMovementEntity>()

    private fun stockEntry(remaining: String, costRemaining: String, status: StockEntryStatus = StockEntryStatus.ACTIVE) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal("10"),
        storageQuantityRemaining = BigDecimal(remaining),
        documentReference = "DLV1",
        detailReference = "DLL1",
        priority = 0,
        unitCost = BigDecimal("10"),
        batchCost = BigDecimal("100.00"),
        costRemaining = BigDecimal(costRemaining),
        status = status
    ).also {
        it.id = stockEntryId
        it.referenceNumber = "STE000001"
    }

    private fun movement(
        movementType: MovementType,
        quantity: String,
        cost: String,
        documentReference: String,
        minutesAfterBase: Long = 0,
        context: String? = null
    ) = StockMovementEntity(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = movementType,
        storageMovedQuantity = BigDecimal(quantity),
        wholeLineQuantityText = "text",
        storageRemainingQuantity = BigDecimal.ZERO,
        movedCost = BigDecimal(cost),
        storageMovedBreakdownText = "text",
        storageRemainingBreakdownText = "text",
        documentReference = documentReference,
        detailReference = "LINE1",
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 1L,
        context = context
    ).also { it.createdOn = baseTime.plusMinutes(minutesAfterBase) }

    private fun received() = movement(MovementType.PURCHASE_RECEIVED, "10", "100.00", "DLV1")

    private fun reset(
        stockEntry: StockEntryEntity,
        movementsOnEntry: List<StockMovementEntity>,
        productBalance: String
    ): List<EntryConsumption> {
        val stockEntryIds = setOf(stockEntryId)
        `when`(stockEntryRepository.findLocationProductIdsByIdIn(anyCollection())).thenReturn(setOf(locationProductId))
        `when`(stockEntryRepository.findAllById(anyIterable())).thenReturn(listOf(stockEntry))
        `when`(stockMovementRepository.findByStockEntryIdInOrderByCreatedOnAscIdAsc(stockEntryIds)).thenReturn(movementsOnEntry)
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(mapOf(locationProductId to BigDecimal(productBalance)))
        `when`(stockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(
            mapOf(locationProductId to QuantityBreakdown.forBaseUnit(listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, baseTime)), unitId))
        )
        `when`(stockMovementRepository.saveAll(anyIterable<StockMovementEntity>())).thenAnswer { invocation ->
            val movements = invocation.getArgument<Iterable<StockMovementEntity>>(0).toList()
            savedMovements.addAll(movements)
            movements
        }
        return stockEntryResetter.reset(stockEntryIds, stockRevisionReferenceNumber)
    }

    private fun assertNumber(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")

    @Test
    fun `a consumed entry is restored by reversing its draw and then drained to zero`() {
        val stockEntry = stockEntry("6", "60.00")

        val entryConsumptions = reset(
            stockEntry, listOf(received(), movement(MovementType.SALE, "4", "40.00", "SALE1", 1)), productBalance = "6"
        )

        assertEquals(1, entryConsumptions.size)
        assertEquals(listOf(MovementType.REVERSED_BY_REVISION, MovementType.STOCK_REVISION_ZEROING), savedMovements.map { it.movementType })
        val reversal = savedMovements[0]
        assertNumber("4", reversal.storageMovedQuantity)
        assertNumber("40.00", reversal.movedCost)
        assertEquals("SALE1", reversal.documentReference)
        assertEquals("LINE1", reversal.detailReference)
        assertNumber("10", reversal.storageRemainingQuantity)
        val drain = savedMovements[1]
        assertNumber("10", drain.storageMovedQuantity)
        assertNumber("100.00", drain.movedCost)
        assertEquals("DLV1", drain.documentReference)
        assertEquals("DLL1", drain.detailReference)
        assertEquals(StockMovementContexts.drainedByStockRevision(stockRevisionReferenceNumber), drain.context)
        assertEquals(StockMovementContexts.restoredForStockRevision(stockRevisionReferenceNumber), reversal.context)
        assertNumber("0", drain.storageRemainingQuantity)
        assertNumber("0", stockEntry.storageQuantityRemaining)
        assertNumber("0", stockEntry.costRemaining)
    }

    @Test
    fun `the product lock is taken before any stock is read`() {
        reset(stockEntry("10", "100.00"), listOf(received()), productBalance = "10")

        val lockThenReads = inOrder(entityAdvisoryLock, stockEntryRepository, stockMovementRepository)
        lockThenReads.verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        lockThenReads.verify(stockEntryRepository).findAllById(anyIterable())
        lockThenReads.verify(stockMovementRepository, atLeastOnce()).findByStockEntryIdInOrderByCreatedOnAscIdAsc(setOf(stockEntryId))
    }

    @Test
    fun `an entry nobody drew from is only drained`() {
        val stockEntry = stockEntry("10", "100.00")

        val entryConsumptions = reset(stockEntry, listOf(received()), productBalance = "10")

        assertTrue(entryConsumptions.isEmpty())
        assertEquals(listOf(MovementType.STOCK_REVISION_ZEROING), savedMovements.map { it.movementType })
        assertNumber("100.00", savedMovements.single().movedCost)
    }

    @Test
    fun `a zero-quantity entry writes no drain movement`() {
        val stockEntry = stockEntry("0", "0.00")

        reset(stockEntry, listOf(received()), productBalance = "0")

        assertTrue(savedMovements.isEmpty())
    }

    @Test
    fun `a transfer is restored with a reversed by revision movement`() {
        reset(
            stockEntry("6", "60.00"),
            listOf(received(), movement(MovementType.TRANSFER_OUT, "4", "40.00", "TRF1", 1)),
            productBalance = "6"
        )

        assertEquals(MovementType.REVERSED_BY_REVISION, savedMovements.first().movementType)
    }

    @Test
    fun `an adjustment is restored with a reversed by revision movement`() {
        reset(
            stockEntry("7", "70.00"),
            listOf(received(), movement(MovementType.STOCK_ADJUSTMENT_DECREASE, "3", "30.00", "ADJ1", 1)),
            productBalance = "7"
        )

        assertEquals(MovementType.REVERSED_BY_REVISION, savedMovements.first().movementType)
    }

    @Test
    fun `a consumer that cannot be reversed is rejected`() {
        val exception = assertThrows(RtsGenericException::class.java) {
            reset(
                stockEntry("7", "70.00"),
                listOf(received(), movement(MovementType.SUPPLIER_RETURN, "3", "30.00", "SRT1", 1)),
                productBalance = "7"
            )
        }

        assertEquals(1, (exception.payload as List<*>).size)
        assertTrue(savedMovements.isEmpty())
    }

    @Test
    fun `a partly reversed draw restores only what is still out`() {
        reset(
            stockEntry("7", "70.00"),
            listOf(
                received(),
                movement(MovementType.SALE, "4", "40.00", "SALE1", 1),
                movement(MovementType.SALE_VOID, "1", "10.00", "SALE1", 2)
            ),
            productBalance = "7"
        )

        assertEquals(MovementType.REVERSED_BY_REVISION, savedMovements.first().movementType)
        assertNumber("3", savedMovements.first().storageMovedQuantity)
        assertNumber("30.00", savedMovements.first().movedCost)
    }

    @Test
    fun `an entry that was already replaced is rejected`() {
        val exception = assertThrows(RtsGenericException::class.java) {
            reset(
                stockEntry("0", "0.00", StockEntryStatus.REPLACED),
                listOf(
                    received(),
                    movement(MovementType.SALE, "4", "40.00", "SALE1", 1),
                    movement(MovementType.REVERSED_BY_REVISION, "4", "40.00", "SALE1", 2),
                    movement(MovementType.STOCK_REVISION_ZEROING, "10", "100.00", "DLV1", 3)
                ),
                productBalance = "0"
            )
        }

        assertTrue((exception.payload as List<*>).single().toString().contains("was already replaced"))
        assertTrue(savedMovements.isEmpty())
    }

    @Test
    fun `a reset entry is marked replaced even when it has nothing to drain`() {
        val stockEntry = stockEntry("0", "0.00")

        reset(stockEntry, listOf(received()), productBalance = "0")

        assertEquals(StockEntryStatus.REPLACED, stockEntry.status)
        assertTrue(savedMovements.isEmpty())
    }

    @Test
    fun `a drained entry is marked replaced`() {
        val stockEntry = stockEntry("10", "100.00")

        reset(stockEntry, listOf(received()), productBalance = "10")

        assertEquals(StockEntryStatus.REPLACED, stockEntry.status)
    }

    @Test
    fun `an unknown stock entry is rejected`() {
        `when`(stockEntryRepository.findAllById(anyIterable())).thenReturn(emptyList())

        assertThrows(RtsGenericException::class.java) {
            stockEntryResetter.reset(setOf(stockEntryId), stockRevisionReferenceNumber)
        }
    }
}
