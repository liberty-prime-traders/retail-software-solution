package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockReplacementLayerPreviewerTest {

    private val locationProductId = UUID.randomUUID()
    private val baseTime = OffsetDateTime.parse("2026-09-01T08:00:00Z")
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockReplacementLayerPreviewer = StockReplacementLayerPreviewer(stockEntryRepository)

    private fun layer(reference: String, remaining: String, activatedOn: OffsetDateTime) = StockEntryEntity(
        locationProductId = locationProductId,
        storageBatchSize = BigDecimal("10"),
        storageQuantityRemaining = BigDecimal(remaining),
        priority = 0,
        unitCost = BigDecimal.ONE,
        batchCost = BigDecimal("10.00"),
        costRemaining = BigDecimal(remaining),
        activatedOn = activatedOn,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = reference
    }

    private fun replacementRequest(revisedStockEntryId: UUID) = StockReplacementLayerRequest(
        locationProductId = locationProductId,
        sourceType = StockItemSource.PURCHASE,
        movementType = MovementType.PURCHASE_RECEIVED,
        documentReference = "DEL000001",
        detailReference = "PREVIEW",
        stockRevisionReferenceNumber = "PREVIEW",
        userProvidedQuantity = BigDecimal("8"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        batchCost = BigDecimal("96.00"),
        revisedStockEntryId = revisedStockEntryId,
    )

    @Test
    fun `the revised layer is left out and the replacement takes its place in the draw order`() {
        val olderLayer = layer("STE000001", "3", baseTime)
        val revisedLayer = layer("STE000002", "6", baseTime.plusDays(1))
        val laterLayer = layer("STE000003", "10", baseTime.plusDays(2))
        `when`(stockEntryRepository.findById(revisedLayer.id!!)).thenReturn(Optional.of(revisedLayer))
        `when`(stockEntryRepository.findFifoEntriesForProducts(listOf(locationProductId)))
            .thenReturn(listOf(laterLayer, revisedLayer, olderLayer))

        val preview = stockReplacementLayerPreviewer.previewReplacement(replacementRequest(revisedLayer.id!!))

        val fifoEntries = preview.fifoEntries
        assertEquals(listOf("STE000001", "PREVIEW", "STE000003"), fifoEntries.map { it.referenceNumber })
        assertEquals(0, BigDecimal("8").compareTo(fifoEntries[1].fifoLayer.storageQuantityRemaining))
        assertEquals(fifoEntries[1].fifoLayer.stockEntryId, preview.replacementStockEntryId)
    }
}
