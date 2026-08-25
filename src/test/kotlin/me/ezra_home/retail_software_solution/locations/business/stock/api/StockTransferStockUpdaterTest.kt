package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockTransferStockUpdaterTest {

    @Test
    fun `consumeStockForDispatch accumulates a running balance across multiple dispatch lines for the same product`() {
        val locationProductId = UUID.randomUUID()
        val firstDispatchLineRequest = StockTransferDispatchLineStockRequest(
            dispatchLineRef = "DISPATCH-LINE-1",
            locationProductId = locationProductId,
            baseQuantity = BigDecimal("10.0000"),
            unitId = UUID.randomUUID(),
            unitCost = BigDecimal("5.00"),
            conversionFactor = BigDecimal.ONE,
            baseUnitId = UUID.randomUUID(),
        )
        val secondDispatchLineRequest = StockTransferDispatchLineStockRequest(
            dispatchLineRef = "DISPATCH-LINE-2",
            locationProductId = locationProductId,
            baseQuantity = BigDecimal("15.0000"),
            unitId = UUID.randomUUID(),
            unitCost = BigDecimal("6.00"),
            conversionFactor = BigDecimal.ONE,
            baseUnitId = UUID.randomUUID(),
        )
        val locationProductIds = listOf(locationProductId, locationProductId)

        val stockEntry = StockEntryEntity(
            locationProductId = locationProductId,
            batchSize = BigDecimal("100.0000"),
            quantityRemaining = BigDecimal("100.0000"),
            priority = 0,
            unitCost = BigDecimal("5.00"),
        )
        stockEntry.id = UUID.randomUUID()

        val stockEntryRepository = mock(StockEntryRepository::class.java)
        val stockMovementRepository = mock(StockMovementRepository::class.java)
        val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
        val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
        val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)

        `when`(stockEntryRepository.findFifoEntriesForProducts(locationProductIds)).thenReturn(listOf(stockEntry))
        `when`(stockBalanceFetcher.getLatestBalances(locationProductIds))
            .thenReturn(mapOf(locationProductId to BigDecimal("100.0000")))
        `when`(locationProductDataFetcher.findSummaryByIds(setOf(locationProductId))).thenReturn(emptyMap())

        val stockTransferStockUpdater = StockTransferStockUpdater(
            stockEntryRepository = stockEntryRepository,
            stockMovementRepository = stockMovementRepository,
            locationProductDataFetcher = locationProductDataFetcher,
            stockBalanceFetcher = stockBalanceFetcher,
            entityAdvisoryLock = entityAdvisoryLock,
        )

        stockTransferStockUpdater.consumeStockForDispatch(listOf(firstDispatchLineRequest, secondDispatchLineRequest))

        @Suppress("UNCHECKED_CAST")
        val movementsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<StockMovementEntity>>
        verify(stockMovementRepository).saveAll(movementsCaptor.capture())
        val movementsByDispatchLineRef = movementsCaptor.value.associateBy { it.externalReferenceNumber }

        // Second line's balance must reflect the first line's consumption, not the original pre-dispatch balance.
        assertEquals(BigDecimal("90.0000"), movementsByDispatchLineRef.getValue("DISPATCH-LINE-1").remainingQuantity)
        assertEquals(BigDecimal("75.0000"), movementsByDispatchLineRef.getValue("DISPATCH-LINE-2").remainingQuantity)
    }
}
