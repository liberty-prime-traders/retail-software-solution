package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockEntryFetcherTest {

    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockEntryFetcher = StockEntryFetcher(stockEntryRepository)

    private val locationProductId = UUID.randomUUID()

    private fun layer(reference: String, activatedOn: OffsetDateTime) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal.TEN,
        storageQuantityRemaining = BigDecimal.TEN,
        priority = 0,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal.TEN,
        costRemaining = BigDecimal.TEN,
        activatedOn = activatedOn,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = reference
        it.createdOn = OffsetDateTime.parse("2026-11-20T08:00:00Z")
    }

    @Test
    fun `an unknown stock entry is rejected`() {
        val unknownStockEntryId = UUID.randomUUID()
        `when`(stockEntryRepository.findById(unknownStockEntryId)).thenReturn(Optional.empty())

        assertThrows(RtsGenericException::class.java) { stockEntryFetcher.get(unknownStockEntryId) }
    }

    @Test
    fun `layers with stock left come back in draw order by activation, not by when they were saved`() {
        val laterDelivery = layer("STE3", OffsetDateTime.parse("2026-10-03T08:00:00Z"))
        val replacementOfEarlier = layer("STE9", OffsetDateTime.parse("2026-10-01T08:00:00Z"))
        `when`(stockEntryRepository.findFifoEntriesForProducts(anyCollection())).thenReturn(listOf(laterDelivery, replacementOfEarlier))

        val layersInDrawOrder = stockEntryFetcher.findWithStockLeftInDrawOrder(locationProductId)

        assertEquals(listOf("STE9", "STE3"), layersInDrawOrder.map { it.referenceNumber })
    }
}
