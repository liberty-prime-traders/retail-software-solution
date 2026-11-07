package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class StockReplacementLayerRecorderTest {

    private val stockIncreaseRecorder = mock(StockIncreaseRecorder::class.java)
    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockReplacementLayerRecorder = StockReplacementLayerRecorder(
        stockIncreaseRecorder,
        stockEntryRepository
    )

    private val locationProductId = UUID.randomUUID()
    private val revisedStockEntryId = UUID.randomUUID()
    private val revisedActivatedOn = OffsetDateTime.parse("2026-09-01T08:00:00Z")

    private fun replacementRequest() = StockReplacementLayerRequest(
        locationProductId = locationProductId,
        sourceType = StockItemSource.PURCHASE,
        movementType = MovementType.PURCHASE_RECEIVED,
        documentReference = "DEL000001",
        detailReference = "DELN000002",
        stockRevisionReferenceNumber = "STRV000001",
        userProvidedQuantity = BigDecimal("4"),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        batchCost = BigDecimal("40.0000"),
        revisedStockEntryId = revisedStockEntryId,
    )

    private fun stubRevisedEntryAndCaptureSpecs(replacementStockEntryId: UUID): List<InboundLayerSpec> {
        val revisedStockEntry = StockEntryEntity(
            locationProductId = locationProductId,
            storageBatchSize = BigDecimal("4"),
            storageQuantityRemaining = BigDecimal.ZERO,
            priority = 0,
            unitCost = BigDecimal.TEN,
            batchCost = BigDecimal("40.0000"),
            costRemaining = BigDecimal.ZERO,
            activatedOn = revisedActivatedOn,
        )
        `when`(stockEntryRepository.findById(revisedStockEntryId)).thenReturn(Optional.of(revisedStockEntry))
        val recordedSpecs = mutableListOf<InboundLayerSpec>()
        `when`(stockIncreaseRecorder.record(anyList())).thenAnswer { invocation ->
            recordedSpecs.addAll(invocation.getArgument<List<InboundLayerSpec>>(0))
            listOf(replacementStockEntryId)
        }
        return recordedSpecs
    }

    @Test
    fun `the layer keeps the document reference, takes the new line reference and names the revision in its context`() {
        val replacementStockEntryId = UUID.randomUUID()
        val recordedSpecs = stubRevisedEntryAndCaptureSpecs(replacementStockEntryId)

        val returnedStockEntryId = stockReplacementLayerRecorder.record(replacementRequest())

        val recordedSpec = recordedSpecs.single()
        assertEquals(replacementStockEntryId, returnedStockEntryId)
        assertEquals("DEL000001", recordedSpec.documentReference)
        assertEquals("DELN000002", recordedSpec.detailReference)
        assertEquals(MovementType.PURCHASE_RECEIVED, recordedSpec.movementType)
        assertEquals("Replacement from stock revision STRV000001", recordedSpec.context)
    }

    @Test
    fun `the layer takes the place of the revised layer in the draw order`() {
        val recordedSpecs = stubRevisedEntryAndCaptureSpecs(UUID.randomUUID())

        stockReplacementLayerRecorder.record(replacementRequest())

        assertEquals(revisedActivatedOn, recordedSpecs.single().activatedOn)
        assertEquals(0, recordedSpecs.single().priority)
    }
}
