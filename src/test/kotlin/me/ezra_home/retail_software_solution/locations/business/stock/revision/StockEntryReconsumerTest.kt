package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.BreakdownUnit
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import me.ezra_home.retail_software_solution.util.quantity.QuantityAsEntered
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockEntryReconsumerTest {

    private val locationProductId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-11-01T10:00:00Z")
    private val replacedStockEntryId = UUID.randomUUID()

    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val stockBreakdownProvider = mock(StockBreakdownProvider::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val stockEntryReconsumer = StockEntryReconsumer(
        mock(EntityAdvisoryLock::class.java), stockEntryRepository, stockMovementRepository,
        stockBalanceFetcher, stockBreakdownProvider, locationProductDataFetcher
    )

    private fun givenStockEntries(vararg stockEntries: StockEntryEntity) {
        val quantityBreakdown = QuantityBreakdown.forBaseUnit(listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, baseTime)), unitId)
        `when`(stockEntryRepository.findFifoEntriesForProducts(anyCollection())).thenReturn(stockEntries.toList())
        `when`(stockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(mapOf(locationProductId to quantityBreakdown))
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(
            mapOf(locationProductId to stockEntries.sumOf { it.storageQuantityRemaining })
        )
        `when`(locationProductDataFetcher.findSummaryByIds(anyCollection())).thenReturn(
            mapOf(locationProductId to LocationProductSummaryDto(locationProductId, "LP1", UUID.randomUUID(), "Sugar", "Pantry", unitId))
        )
    }

    private fun givenReplacementLayer(replacement: StockEntryEntity) {
        `when`(stockEntryRepository.findById(replacement.id!!)).thenReturn(Optional.of(replacement))
    }

    private fun stockEntry(
        reference: String,
        quantity: String,
        batchCost: String,
        status: StockEntryStatus = StockEntryStatus.ACTIVE,
        entryLocationProductId: UUID = locationProductId
    ) = StockEntryEntity(
        locationProductId = entryLocationProductId,
        storageBatchSize = BigDecimal(quantity),
        storageQuantityRemaining = BigDecimal(quantity),
        priority = 0,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal(batchCost),
        costRemaining = BigDecimal(batchCost),
        status = status,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = reference
    }

    private fun consumption(documentReference: String, quantity: String, cost: String) = StockEntryConsumption(
        stockEntryId = replacedStockEntryId,
        locationProductId = locationProductId,
        consumer = StockConsumer(StockMovementOrigin(documentReference, "SALL1"), MovementType.SALE, null),
        quantityAsEntered = QuantityAsEntered(unitId, 1L, "whole line of $documentReference"),
        orderOfConsumption = OrderOfConsumption(null, null),
        netConsumedStorageQuantity = ExactQuantity.ofStorage(BigDecimal(quantity)),
        netConsumedCost = BigDecimal(cost)
    )

    @Test
    fun `the plans are applied after the whole replay is known good, drawing the layers down and saving the movements`() {
        val replacement = stockEntry("STE2", "10", "120.00")
        givenStockEntries(replacement)
        givenReplacementLayer(replacement)

        val consumerReplays = stockEntryReconsumer.reconsume(listOf(consumption("SAL1", "4", "40.00")), replacement.id!!, "STRV000001")

        assertEquals(BigDecimal("6.0000"), replacement.storageQuantityRemaining)
        assertEquals(BigDecimal("72.00"), replacement.costRemaining)
        assertEquals(BigDecimal("48.00"), consumerReplays.single().newCost)
        assertEquals(listOf(replacement.id), consumerReplays.single().movements.map { it.stockEntryId })
        verify(stockMovementRepository).saveAll(consumerReplays.single().movements)
    }

    @Test
    fun `when any consumer cannot be covered nothing is drawn, saved or changed, even for consumers that could be`() {
        val replacement = stockEntry("STE2", "5", "60.00")
        givenStockEntries(replacement)
        givenReplacementLayer(replacement)

        val exception = assertThrows(RtsGenericException::class.java) {
            stockEntryReconsumer.reconsume(
                listOf(consumption("SAL1", "4", "40.00"), consumption("SAL2", "4", "40.00")),
                replacement.id!!,
                "STRV000001"
            )
        }

        assertEquals(1, (exception.payload as List<*>).size)
        assertEquals(BigDecimal("5"), replacement.storageQuantityRemaining)
        assertEquals(BigDecimal("60.00"), replacement.costRemaining)
        verify(stockEntryRepository, never()).saveAll(anyCollection<StockEntryEntity>())
        verify(stockMovementRepository, never()).saveAll(anyCollection<StockMovementEntity>())
    }

    @Test
    fun `a replacement that was never saved is refused even when later layers could cover every consumer`() {
        val later = stockEntry("STE3", "10", "90.00")
        givenStockEntries(later)

        assertThrows(IllegalArgumentException::class.java) {
            stockEntryReconsumer.reconsume(listOf(consumption("SAL1", "4", "40.00")), UUID.randomUUID(), "STRV000001")
        }

        assertEquals(BigDecimal("10"), later.storageQuantityRemaining)
        verify(stockMovementRepository, never()).saveAll(anyCollection<StockMovementEntity>())
    }

    @Test
    fun `a replacement that is already replaced or belongs to another product is refused`() {
        givenStockEntries(stockEntry("STE3", "10", "90.00"))
        val alreadyReplaced = stockEntry("STE2", "0", "0.00", status = StockEntryStatus.REPLACED)
        val ofAnotherProduct = stockEntry("STE4", "0", "0.00", entryLocationProductId = UUID.randomUUID())
        givenReplacementLayer(alreadyReplaced)
        givenReplacementLayer(ofAnotherProduct)

        assertThrows(IllegalArgumentException::class.java) {
            stockEntryReconsumer.reconsume(listOf(consumption("SAL1", "4", "40.00")), alreadyReplaced.id!!, "STRV000001")
        }
        assertThrows(IllegalArgumentException::class.java) {
            stockEntryReconsumer.reconsume(listOf(consumption("SAL1", "4", "40.00")), ofAnotherProduct.id!!, "STRV000001")
        }

        verify(stockMovementRepository, never()).saveAll(anyCollection<StockMovementEntity>())
    }

    @Test
    fun `a zero quantity replacement is accepted and later layers cover the consumers`() {
        val zeroReplacement = stockEntry("STE2", "0", "0.00")
        val later = stockEntry("STE3", "10", "90.00")
        givenStockEntries(later)
        givenReplacementLayer(zeroReplacement)

        val consumerReplays = stockEntryReconsumer.reconsume(listOf(consumption("SAL1", "4", "40.00")), zeroReplacement.id!!, "STRV000001")

        assertEquals(listOf(later.id), consumerReplays.single().movements.map { it.stockEntryId })
    }
}
