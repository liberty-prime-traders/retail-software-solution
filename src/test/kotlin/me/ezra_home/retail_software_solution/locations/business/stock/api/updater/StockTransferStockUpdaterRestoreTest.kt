package me.ezra_home.retail_software_solution.locations.business.stock.api.updater

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.NetStockDraw
import me.ezra_home.retail_software_solution.locations.business.stock.NetStockDrawReader
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockTransferStockUpdaterRestoreTest {

    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val netStockDrawReader = mock(NetStockDrawReader::class.java)
    private val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java)
    private val stockTransferStockUpdater = StockTransferStockUpdater(
        stockEntryRepository = stockEntryRepository,
        stockMovementRepository = stockMovementRepository,
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        stockBalanceFetcher = stockBalanceFetcher,
        productStockBreakdownProvider = productStockBreakdownProvider,
        entityAdvisoryLock = mock(EntityAdvisoryLock::class.java),
        netStockDrawReader = netStockDrawReader
    )

    @Test
    fun `restoring no lines reads and writes nothing`() {
        stockTransferStockUpdater.restoreStockForLines("DISPATCH-1", emptyList())

        verifyNoInteractions(netStockDrawReader, stockEntryRepository, stockBalanceFetcher)
    }

    @Test
    fun `restoring lines that have no net draw left, as on a repeated cancel, writes nothing`() {
        `when`(netStockDrawReader.lockAndReadNetDrawsOfLines("DISPATCH-1", listOf("DL-1"), MovementType.TRANSFER_OUT))
            .thenReturn(emptyList())

        stockTransferStockUpdater.restoreStockForLines("DISPATCH-1", listOf("DL-1"))

        verifyNoInteractions(stockEntryRepository, stockBalanceFetcher)
    }

    @Test
    fun `restoring a draw puts its quantity and cost back on the layer and records a cancel movement on the new balance`() {
        val locationProductId = UUID.randomUUID()
        val pieceId = UUID.randomUUID()
        val stockEntryId = UUID.randomUUID()
        val stockEntry = StockEntryEntity(
            locationProductId = locationProductId,
            storageBatchSize = BigDecimal("100.0000"),
            storageQuantityRemaining = BigDecimal("90.0000"),
            priority = 0,
            unitCost = BigDecimal("5.00"),
            batchCost = BigDecimal("500.0000"),
            costRemaining = BigDecimal("450.0000"),
        ).also { it.id = stockEntryId }
        `when`(netStockDrawReader.lockAndReadNetDrawsOfLines("DISPATCH-1", listOf("DL-1"), MovementType.TRANSFER_OUT))
            .thenReturn(
                listOf(
                    NetStockDraw(
                        stockEntryId = stockEntryId,
                        locationProductId = locationProductId,
                        documentReference = "DISPATCH-1",
                        detailReference = "DL-1",
                        storageMovedQuantity = BigDecimal("10.0000"),
                        movedCost = BigDecimal("50.0000"),
                        wholeLineQuantityText = "10",
                        unitId = pieceId,
                        storageUnitsPerUserProvidedUnit = 1L,
                    )
                )
            )
        `when`(stockEntryRepository.findAllById(listOf(stockEntryId))).thenReturn(listOf(stockEntry))
        `when`(stockBalanceFetcher.getLatestBalances(listOf(locationProductId)))
            .thenReturn(mapOf(locationProductId to BigDecimal("90.0000")))
        `when`(productStockBreakdownProvider.forLocationProducts(setOf(locationProductId))).thenReturn(
            mapOf(
                locationProductId to QuantityBreakdown.forBaseUnit(
                    listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.now())), pieceId
                )
            )
        )

        stockTransferStockUpdater.restoreStockForLines("DISPATCH-1", listOf("DL-1"))

        assertSameNumber("100.0000", stockEntry.storageQuantityRemaining)
        assertSameNumber("500.0000", stockEntry.costRemaining)
        val movement = savedMovements().single()
        assertEquals(MovementType.TRANSFER_CANCELLED, movement.movementType)
        assertEquals("DISPATCH-1", movement.documentReference)
        assertEquals("DL-1", movement.detailReference)
        assertSameNumber("10.0000", movement.storageMovedQuantity)
        assertSameNumber("100.0000", movement.storageRemainingQuantity)
        assertSameNumber("50.0000", movement.movedCost)
        verify(stockEntryRepository).saveAll(listOf(stockEntry))
    }

    private fun savedMovements(): List<StockMovementEntity> {
        @Suppress("UNCHECKED_CAST")
        val movementsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<StockMovementEntity>>
        verify(stockMovementRepository).saveAll(movementsCaptor.capture())
        return movementsCaptor.value
    }

    private fun assertSameNumber(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")
}
