package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockIncreaseRecorderTest {

    private val pieceId = UUID.randomUUID()
    private val firstLocationProductId = UUID.randomUUID()
    private val secondLocationProductId = UUID.randomUUID()
    private val pieceBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.parse("2026-01-01T00:00:00Z"))), pieceId
    )

    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockBreakdownProvider = mock(StockBreakdownProvider::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockIncreaseRecorder = StockIncreaseRecorder(
        stockEntryRepository, stockMovementRepository, stockBalanceFetcher, stockBreakdownProvider, entityAdvisoryLock
    )

    private val savedMovements = mutableListOf<StockMovementEntity>()

    init {
        `when`(stockMovementRepository.saveAll(anyList<StockMovementEntity>())).thenAnswer { invocation ->
            invocation.getArgument<List<StockMovementEntity>>(0).also { savedMovements.addAll(it) }
        }
    }

    private fun spec(locationProductId: UUID, quantity: String, detailReference: String) = InboundLayerSpec(
        locationProductId = locationProductId,
        sourceType = StockItemSource.TRANSFER_IN,
        movementType = MovementType.TRANSFER_IN,
        documentReference = "DISPATCH-1",
        detailReference = detailReference,
        storageQuantity = ExactQuantity.ofStorage(BigDecimal(quantity)),
        batchCost = BigDecimal("10.0000"),
        userProvidedQuantity = BigDecimal(quantity),
        unitId = pieceId,
        storageUnitsPerUserProvidedUnit = 1L,
    )

    @Test
    fun `each product's running balance carries across its own lines and never leaks into another product`() {
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to BigDecimal("100"), secondLocationProductId to BigDecimal("5"))
        )
        `when`(stockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to pieceBreakdown, secondLocationProductId to pieceBreakdown)
        )
        `when`(stockEntryRepository.save(any(StockEntryEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<StockEntryEntity>(0).also { it.id = UUID.randomUUID() }
        }

        stockIncreaseRecorder.record(
            listOf(
                spec(firstLocationProductId, "10", "LINE-1"),
                spec(secondLocationProductId, "3", "LINE-2"),
                spec(firstLocationProductId, "20", "LINE-3"),
            )
        )

                assertEquals(
            listOf(BigDecimal("110.0000"), BigDecimal("8.0000"), BigDecimal("130.0000")),
            savedMovements.map { it.storageRemainingQuantity }
        )
        assertEquals(listOf("LINE-1", "LINE-2", "LINE-3"), savedMovements.map { it.detailReference })
    }

    @Test
    fun `no specs means no lock, no read and no write`() {
        stockIncreaseRecorder.record(emptyList())

        verifyNoInteractions(entityAdvisoryLock, stockBalanceFetcher, stockEntryRepository, stockMovementRepository)
    }
}
