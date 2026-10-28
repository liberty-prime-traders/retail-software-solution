package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class SaleStockUpdaterTest {

    private val locationProductId = UUID.randomUUID()
    private val pieceId = UUID.randomUUID()
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val stockBreakdownProvider = mock(StockBreakdownProvider::class.java)

    private val saleStockUpdater = SaleStockUpdater(
        stockEntryRepository = stockEntryRepository,
        stockMovementRepository = mock(StockMovementRepository::class.java),
        locationProductDataFetcher = locationProductDataFetcher,
        stockBalanceFetcher = stockBalanceFetcher,
        stockBreakdownProvider = stockBreakdownProvider,
        entityAdvisoryLock = mock(EntityAdvisoryLock::class.java),
    )

    @Test
    fun `consumption spanning layers with different costs sums each layer at its own cost`() {
        val saleLineId = UUID.randomUUID()
        givenLayers(listOf("4.0000" to "10.0000", "10.0000" to "30.0000"))

        val costsBySaleLineId = consume(saleLineId, "6.0000")

        assertEquals(BigDecimal("16.00"), costsBySaleLineId.getValue(saleLineId).cost)
    }

    @Test
    fun `a partial take costs a proportion of the layer cost rounded once to cents`() {
        val saleLineId = UUID.randomUUID()
        givenLayers(listOf("3.0000" to "10.0000"))

        val costsBySaleLineId = consume(saleLineId, "1.0000")

        assertEquals(BigDecimal("3.33"), costsBySaleLineId.getValue(saleLineId).cost)
    }

    @Test
    fun `each sale line gets its own cost keyed by sale line id`() {
        val firstLineId = UUID.randomUUID()
        val secondLineId = UUID.randomUUID()
        givenLayers(listOf("10.0000" to "20.0000", "10.0000" to "50.0000"))

        val costsBySaleLineId = saleStockUpdater.consumeStock(
            listOf(request(firstLineId, "10.0000"), request(secondLineId, "2.0000")),
            "SALE-1",
        ).associateBy { it.saleLineId }

        assertEquals(BigDecimal("20.00"), costsBySaleLineId.getValue(firstLineId).cost)
        assertEquals(BigDecimal("10.00"), costsBySaleLineId.getValue(secondLineId).cost)
    }

    @Test
    fun `no requests means no costs`() {
        assertEquals(emptyList<SaleLineConsumedCost>(), saleStockUpdater.consumeStock(emptyList(), "SALE-1"))
    }

    private fun consume(saleLineId: UUID, storageQuantity: String) =
        saleStockUpdater.consumeStock(listOf(request(saleLineId, storageQuantity)), "SALE-1").associateBy { it.saleLineId }

    private fun givenLayers(layers: List<Pair<String, String>>) {
        val stockEntries = layers.mapIndexed { index, (storageQuantityRemaining, batchCost) ->
            StockEntryEntity(
                locationProductId = locationProductId,
                storageBatchSize = BigDecimal(storageQuantityRemaining),
                storageQuantityRemaining = BigDecimal(storageQuantityRemaining),
                priority = index,
                unitCost = BigDecimal.ONE,
                batchCost = BigDecimal(batchCost),
                costRemaining = BigDecimal(batchCost),
            ).also { it.id = UUID.randomUUID() }
        }
        `when`(stockEntryRepository.findFifoEntriesForProducts(anyList()))
            .thenReturn(stockEntries)
        `when`(stockBalanceFetcher.getLatestBalances(anyList()))
            .thenReturn(mapOf(locationProductId to stockEntries.sumOf { it.storageQuantityRemaining }))
        `when`(locationProductDataFetcher.findSummaryByIds(anySet()))
            .thenReturn(emptyMap())
        `when`(stockBreakdownProvider.forLocationProducts(anySet())).thenReturn(
            mapOf(
                locationProductId to QuantityBreakdown.forBaseUnit(
                    listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.now())), pieceId
                )
            )
        )
    }

    private fun request(saleLineId: UUID, storageQuantity: String) = SaleLineStockRequest(
        saleLineId = saleLineId,
        saleLineReferenceNumber = "SALN-$saleLineId".take(30),
        locationProductId = locationProductId,
        userProvidedQuantity = BigDecimal(storageQuantity),
        unitId = pieceId,
        storageUnitsPerUserProvidedUnit = 1L,
    )
}
