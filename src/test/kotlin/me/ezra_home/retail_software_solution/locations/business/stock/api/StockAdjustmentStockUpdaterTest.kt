package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class StockAdjustmentStockUpdaterTest {

    private val locationProductId = UUID.randomUUID()
    private val pieceId = UUID.randomUUID()
    private val pieceBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.parse("2026-01-01T00:00:00Z"))),
        pieceId
    )
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockReserver = mock(StockReserver::class.java)
    private val stockBreakdownProvider = mock(StockBreakdownProvider::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockIncreaseRecorder = mock(StockIncreaseRecorder::class.java)
    private val stockAdjustmentStockUpdater = StockAdjustmentStockUpdater(
        stockIncreaseRecorder,
        stockEntryRepository,
        stockMovementRepository,
        stockBalanceFetcher,
        stockBreakdownProvider,
        StockAvailabilityValidator(stockBalanceFetcher, stockReserver, stockBreakdownProvider, entityAdvisoryLock),
        entityAdvisoryLock,
    )

    @Test
    fun `decrease within one layer takes the proportional layer cost and leaves the rest`() {
        val layer = stockEntry(storageQuantity = "10", batchCost = "30.0000")
        givenStock(balance = "10", layers = listOf(layer))

        val consumedCost = stockAdjustmentStockUpdater.decreaseStock(decreaseRequest("4")).totalCost

        assertEquals(BigDecimal("12.00"), consumedCost)
        assertEquals(0, BigDecimal("6").compareTo(layer.storageQuantityRemaining))
        assertEquals(0, BigDecimal("18").compareTo(layer.costRemaining))
    }

    @Test
    fun `decrease across layers sums the per-layer costs to the total and empties the older layer exactly`() {
        val olderLayer = stockEntry(storageQuantity = "3", batchCost = "10.0000", priority = 0)
        val newerLayer = stockEntry(storageQuantity = "7", batchCost = "21.0000", priority = 1)
        givenStock(balance = "10", layers = listOf(newerLayer, olderLayer))

        val consumedCost = stockAdjustmentStockUpdater.decreaseStock(decreaseRequest("5")).totalCost

        assertEquals(BigDecimal("16.00"), consumedCost)
        assertEquals(0, BigDecimal.ZERO.compareTo(olderLayer.costRemaining))
        assertEquals(0, BigDecimal("15").compareTo(newerLayer.costRemaining))
    }

    @Test
    fun `decrease larger than the available quantity is rejected`() {
        givenStock(balance = "3", layers = listOf(stockEntry(storageQuantity = "3", batchCost = "9.0000")))

        assertThrows(RtsGenericException::class.java) {
            stockAdjustmentStockUpdater.decreaseStock(decreaseRequest("4"))
        }
    }

    private fun givenStock(balance: String, layers: List<StockEntryEntity>) {
        `when`(stockEntryRepository.findFifoEntriesForProducts(anyCollection())).thenReturn(layers)
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(mapOf(locationProductId to BigDecimal(balance)))
        `when`(stockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(mapOf(locationProductId to pieceBreakdown))
    }

    private fun decreaseRequest(storageQuantity: String) = StockAdjustmentDecreaseStockRequest(
        locationProductId = locationProductId,
        productLabel = "Test Product",
        storageQuantity = ExactQuantity.ofStorage(BigDecimal(storageQuantity)),
        userProvidedQuantity = BigDecimal(storageQuantity),
        unitId = pieceId,
        storageUnitsPerUserProvidedUnit = 1L,
        documentReference = "STADJ000001",
        adjustmentDetails = StockAdjustmentMovementDetails(StockAdjustmentCategory.INTERNAL_USE, null, "Shop supplies"),
    )

    private fun stockEntry(storageQuantity: String, batchCost: String, priority: Int = 0) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal(storageQuantity),
        storageQuantityRemaining = BigDecimal(storageQuantity),
        priority = priority,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal(batchCost),
        costRemaining = BigDecimal(batchCost),
    ).also { it.id = UUID.randomUUID() }
}
