package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockReplacementLayerRecorderTest {

    private val stockIncreaseRecorder = mock(StockIncreaseRecorder::class.java)
    private val stockReplacementLayerRecorder = StockReplacementLayerRecorder(stockIncreaseRecorder)

    @Test
    fun `the layer keeps the original document reference, takes the new line reference and names the revision in its context`() {
        val replacementStockEntryId = UUID.randomUUID()
        val recordedSpecs = mutableListOf<InboundLayerSpec>()
        `when`(stockIncreaseRecorder.record(anyList())).thenAnswer { invocation ->
            recordedSpecs.addAll(invocation.getArgument<List<InboundLayerSpec>>(0))
            listOf(replacementStockEntryId)
        }
        val pieceId = UUID.randomUUID()

        val returnedStockEntryId = stockReplacementLayerRecorder.record(
            StockReplacementLayerRequest(
                locationProductId = UUID.randomUUID(),
                sourceType = StockItemSource.PURCHASE,
                movementType = MovementType.PURCHASE_RECEIVED,
                documentReference = "DEL000001",
                detailReference = "DELN000002",
                stockRevisionReferenceNumber = "STRV000001",
                userProvidedQuantity = BigDecimal("4"),
                unitId = pieceId,
                storageUnitsPerUserProvidedUnit = 1L,
                batchCost = BigDecimal("40.0000"),
                priority = -1000,
            )
        )

        val recordedSpec = recordedSpecs.single()
        assertEquals(replacementStockEntryId, returnedStockEntryId)
        assertEquals("DEL000001", recordedSpec.documentReference)
        assertEquals("DELN000002", recordedSpec.detailReference)
        assertEquals(MovementType.PURCHASE_RECEIVED, recordedSpec.movementType)
        assertEquals(-1000, recordedSpec.priority)
        assertEquals("Replacement from stock revision STRV000001", recordedSpec.context)
    }
}
