package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockTransferSchemaGatewayTest {

    private val dispatchId = UUID.randomUUID()
    private val stockTransferDispatchRepository = mock(StockTransferDispatchRepository::class.java)
    private val stockTransferDispatchLineRepository = mock(StockTransferDispatchLineRepository::class.java)
    private val stockTransferStockUpdater = mock(StockTransferStockUpdater::class.java)
    private val stockTransferSchemaGateway = StockTransferSchemaGateway(
        stockTransferDispatchRepository = stockTransferDispatchRepository,
        stockTransferDispatchLineRepository = stockTransferDispatchLineRepository,
        stockTransferReceiptRepository = mock(StockTransferReceiptRepository::class.java),
        stockTransferReceiptLineRepository = mock(StockTransferReceiptLineRepository::class.java),
        reconciledTransferLineFetcher = mock(ReconciledTransferLineFetcher::class.java),
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        stockTransferStockUpdater = stockTransferStockUpdater
    )

    private val dispatch = StockTransferDispatchEntity("ORD-1", StockTransferStatus.DISPATCHED)
        .also { it.id = dispatchId; it.referenceNumber = "DISPATCH-1" }

    private fun dispatchLine(reference: String) = StockTransferDispatchLineEntity(
        stockTransferDispatchId = dispatchId,
        orgProductId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        wholeLineUserProvidedQuantity = BigDecimal("2.0000"),
        storageQuantityDispatched = BigDecimal("36.0000"),
        unitId = UUID.randomUUID(),
        unitCost = BigDecimal("0.2777777778"),
        costDispatched = BigDecimal("10.0000"),
        storageUnitsPerUserProvidedUnit = 18L
    ).also { it.referenceNumber = reference }

    private fun givenDispatchWithLines(vararg lineReferences: String) {
        `when`(stockTransferDispatchRepository.findByStockTransferOrderRef("ORD-1")).thenReturn(dispatch)
        `when`(stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatchId))
            .thenReturn(lineReferences.map { dispatchLine(it) })
    }

    @Test
    fun `cancelling a dispatch restores every dispatch line first, then marks the dispatch cancelled`() {
        givenDispatchWithLines("DL-1", "DL-2")

        val dispatchReference = stockTransferSchemaGateway.cancelDispatch("ORD-1")

        assertEquals("DISPATCH-1", dispatchReference)
        val restoreThenSave = inOrder(stockTransferStockUpdater, stockTransferDispatchRepository)
        restoreThenSave.verify(stockTransferStockUpdater).restoreStockForLines("DISPATCH-1", listOf("DL-1", "DL-2"))
        restoreThenSave.verify(stockTransferDispatchRepository).save(dispatch)
        assertEquals(StockTransferStatus.CANCELLED, dispatch.status)
    }

    @Test
    fun `a restore that is blocked leaves the dispatch dispatched and saves nothing`() {
        givenDispatchWithLines("DL-1")
        doThrow(
            BlockedChangeException("blocked", listOf(BlockingProblem("DISPATCH-1", "reversed more than drawn", "blocked")))
        ).`when`(stockTransferStockUpdater).restoreStockForLines("DISPATCH-1", listOf("DL-1"))

        assertThrows(BlockedChangeException::class.java) { stockTransferSchemaGateway.cancelDispatch("ORD-1") }

        assertEquals(StockTransferStatus.DISPATCHED, dispatch.status)
        verify(stockTransferDispatchRepository, never()).save(dispatch)
    }

    @Test
    fun `a draft dispatch has no lines to restore and is simply cancelled`() {
        givenDispatchWithLines()

        stockTransferSchemaGateway.cancelDispatch("ORD-1")

        verify(stockTransferStockUpdater).restoreStockForLines("DISPATCH-1", emptyList())
        assertEquals(StockTransferStatus.CANCELLED, dispatch.status)
    }
}
