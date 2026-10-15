package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SaleStockUpdaterTest {

    private val locationProductId = UUID.randomUUID()
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)

    private val saleStockUpdater = SaleStockUpdater(
        stockEntryRepository = stockEntryRepository,
        stockMovementRepository = mock(StockMovementRepository::class.java),
        locationProductDataFetcher = locationProductDataFetcher,
        stockBalanceFetcher = stockBalanceFetcher,
        entityAdvisoryLock = mock(EntityAdvisoryLock::class.java),
    )

    @Test
    fun `consumption spanning layers with different costs sums each layer at its own unit cost`() {
        val saleLineId = UUID.randomUUID()
        givenLayers(listOf("4.0000" to "2.5000", "10.0000" to "3.0000"))

        val costsBySaleLineId = consume(saleLineId, "6.0000")

        assertEquals(BigDecimal("16.0000"), costsBySaleLineId.getValue(saleLineId).cost)
    }

    @Test
    fun `cost is rounded once to scale 4 after summing every layer`() {
        val saleLineId = UUID.randomUUID()
        givenLayers(listOf("0.5000" to "0.0003", "0.5000" to "0.0003"))

        val costsBySaleLineId = consume(saleLineId, "1.0000")

        // Each layer costs 0.00015; rounding per layer would give 0.0004, summing first gives 0.0003.
        assertEquals(BigDecimal("0.0003"), costsBySaleLineId.getValue(saleLineId).cost)
    }

    @Test
    fun `each sale line gets its own cost keyed by sale line id`() {
        val firstLineId = UUID.randomUUID()
        val secondLineId = UUID.randomUUID()
        givenLayers(listOf("10.0000" to "2.0000", "10.0000" to "5.0000"))

        val costsBySaleLineId = saleStockUpdater.consumeStock(
            listOf(request(firstLineId, "10.0000"), request(secondLineId, "2.0000")),
            "SALE-1",
        ).associateBy { it.saleLineId }

        assertEquals(BigDecimal("20.0000"), costsBySaleLineId.getValue(firstLineId).cost)
        assertEquals(BigDecimal("10.0000"), costsBySaleLineId.getValue(secondLineId).cost)
    }

    @Test
    fun `no requests means no costs`() {
        assertEquals(emptyList<SaleLineConsumedCost>(), saleStockUpdater.consumeStock(emptyList(), "SALE-1"))
    }

    private fun consume(saleLineId: UUID, baseQuantity: String) =
        saleStockUpdater.consumeStock(listOf(request(saleLineId, baseQuantity)), "SALE-1").associateBy { it.saleLineId }

    private fun givenLayers(layers: List<Pair<String, String>>) {
        val stockEntries = layers.mapIndexed { index, (quantityRemaining, unitCost) ->
            StockEntryEntity(
                locationProductId = locationProductId,
                batchSize = BigDecimal(quantityRemaining),
                quantityRemaining = BigDecimal(quantityRemaining),
                priority = index,
                unitCost = BigDecimal(unitCost),
            ).also { it.id = UUID.randomUUID() }
        }
        `when`(stockEntryRepository.findFifoEntriesForProducts(anyList()))
            .thenReturn(stockEntries)
        `when`(stockBalanceFetcher.getLatestBalances(anyList()))
            .thenReturn(mapOf(locationProductId to stockEntries.sumOf { it.quantityRemaining }))
        `when`(locationProductDataFetcher.findSummaryByIds(anySet()))
            .thenReturn(emptyMap())
    }

    private fun request(saleLineId: UUID, baseQuantity: String) = SaleLineStockRequest(
        saleLineId = saleLineId,
        locationProductId = locationProductId,
        baseQuantity = BigDecimal(baseQuantity),
        unitId = UUID.randomUUID(),
        conversionRatio = ConversionRatio.IDENTITY,
    )
}
