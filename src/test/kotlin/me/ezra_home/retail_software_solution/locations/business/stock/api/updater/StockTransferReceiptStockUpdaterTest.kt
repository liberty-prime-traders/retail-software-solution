package me.ezra_home.retail_software_solution.locations.business.stock.api.updater

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductIdentityDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class StockTransferReceiptStockUpdaterTest {

    private val pieceId = UUID.randomUUID()
    private val storageRootId = UUID.randomUUID()
    private val firstOrgProductId = UUID.randomUUID()
    private val secondOrgProductId = UUID.randomUUID()
    private val firstLocationProductId = UUID.randomUUID()
    private val secondLocationProductId = UUID.randomUUID()

    private val stockIncreaseRecorder = mock(StockIncreaseRecorder::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val unitConversionGraphFacade = mock(UnitConversionGraphFacade::class.java)
    private val unitConversionGraph = mock(UnitConversionGraph::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockTransferReceiptStockUpdater = StockTransferReceiptStockUpdater(
        stockIncreaseRecorder, stockMovementRepository, locationProductDataFetcher, unitConversionGraphFacade,
        entityAdvisoryLock
    )

    private val recordedLayerSpecs = mutableListOf<InboundLayerSpec>()

    init {
        doAnswer { invocation ->
            recordedLayerSpecs.addAll(invocation.getArgument<List<InboundLayerSpec>>(0))
            emptyList<UUID>()
        }.`when`(stockIncreaseRecorder).record(anyList())
    }

    private fun line(orgProductId: UUID, lineReference: String, storageQuantity: String, cost: String, wholeLineQuantity: String, ratio: Long) =
        StockTransferReceiptLineStockRequest(
            dispatchLineReferenceNumber = lineReference,
            orgProductId = orgProductId,
            wholeLineUserProvidedQuantity = BigDecimal(wholeLineQuantity),
            storageQuantityDispatched = BigDecimal(storageQuantity),
            costDispatched = BigDecimal(cost),
            unitId = pieceId,
            storageUnitsPerUserProvidedUnit = ratio,
        )

    private fun givenDestinationProducts(destinationBaseUnitId: UUID = pieceId) {
        `when`(locationProductDataFetcher.findIdentitiesByOrgProductIds(anyCollection())).thenReturn(
            mapOf(
                firstOrgProductId to LocationProductIdentityDto(firstLocationProductId, firstOrgProductId),
                secondOrgProductId to LocationProductIdentityDto(secondLocationProductId, secondOrgProductId),
            )
        )
        `when`(locationProductDataFetcher.getBaseUnitIds(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to destinationBaseUnitId, secondLocationProductId to destinationBaseUnitId)
        )
        `when`(unitConversionGraphFacade.getOrLoad()).thenReturn(unitConversionGraph)
        `when`(unitConversionGraph.getStorageUnitId(pieceId)).thenReturn(storageRootId)
    }

    @Test
    fun `recordTransferReceipt hands the recorder one transfer-in layer spec per line with its quantity, cost and references`() {
        givenDestinationProducts()

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            "DISPATCH-1",
            listOf(
                line(firstOrgProductId, "DL-1", "36", "100.0000", "2", 18L),
                line(secondOrgProductId, "DL-2", "5", "7.5000", "5", 1L),
                line(firstOrgProductId, "DL-3", "4", "9.0000", "4", 1L),
            )
        )

        assertEquals(
            listOf(
                InboundLayerSpec(
                    firstLocationProductId, StockItemSource.TRANSFER_IN, MovementType.TRANSFER_IN, "DISPATCH-1", "DL-1",
                    ExactQuantity.ofStorage(BigDecimal("36")), BigDecimal("100.0000"), BigDecimal("2"), pieceId, 18L
                ),
                InboundLayerSpec(
                    secondLocationProductId, StockItemSource.TRANSFER_IN, MovementType.TRANSFER_IN, "DISPATCH-1", "DL-2",
                    ExactQuantity.ofStorage(BigDecimal("5")), BigDecimal("7.5000"), BigDecimal("5"), pieceId, 1L
                ),
                InboundLayerSpec(
                    firstLocationProductId, StockItemSource.TRANSFER_IN, MovementType.TRANSFER_IN, "DISPATCH-1", "DL-3",
                    ExactQuantity.ofStorage(BigDecimal("4")), BigDecimal("9.0000"), BigDecimal("4"), pieceId, 1L
                ),
            ),
            recordedLayerSpecs
        )
    }

    @Test
    fun `a destination product counted in a different storage unit rejects the receipt before any stock is recorded`() {
        givenDestinationProducts(destinationBaseUnitId = UUID.randomUUID())

        val exception = assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptStockUpdater.recordTransferReceipt(
                "DISPATCH-1", listOf(line(firstOrgProductId, "DL-1", "1", "1.0000", "1", 1L))
            )
        }

        assertEquals(StockTransferReceiptStockUpdater.SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER, exception.message)
        verifyNoInteractions(stockIncreaseRecorder)
    }

    @Test
    fun `a line whose product does not exist at the destination rejects the receipt`() {
        `when`(locationProductDataFetcher.findIdentitiesByOrgProductIds(anyCollection())).thenReturn(emptyMap())

        assertThrows(RtsGenericException::class.java) {
            stockTransferReceiptStockUpdater.recordTransferReceipt(
                "DISPATCH-1", listOf(line(firstOrgProductId, "DL-1", "1", "1.0000", "1", 1L))
            )
        }

        verifyNoInteractions(stockIncreaseRecorder)
    }

    @Test
    fun `a line that already has a transfer-in movement is not credited again`() {
        givenDestinationProducts()
        `when`(
            stockMovementRepository.findPresentDetailReferences(
                "DISPATCH-1", listOf("DL-1", "DL-2"), MovementType.TRANSFER_IN
            )
        ).thenReturn(setOf("DL-1"))

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            "DISPATCH-1",
            listOf(
                line(firstOrgProductId, "DL-1", "36", "100.0000", "2", 18L),
                line(secondOrgProductId, "DL-2", "5", "7.5000", "5", 1L),
            )
        )

        assertEquals(listOf("DL-2"), recordedLayerSpecs.map { it.detailReference })
    }

    @Test
    fun `a dispatch whose lines are all already credited records nothing`() {
        givenDestinationProducts()
        `when`(
            stockMovementRepository.findPresentDetailReferences("DISPATCH-1", listOf("DL-1"), MovementType.TRANSFER_IN)
        ).thenReturn(setOf("DL-1"))

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            "DISPATCH-1",
            listOf(line(firstOrgProductId, "DL-1", "1", "1.0000", "1", 1L))
        )

        verifyNoInteractions(stockIncreaseRecorder)
    }

    @Test
    fun `the product lock is taken before the credited lines are looked up`() {
        givenDestinationProducts()

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            "DISPATCH-1",
            listOf(
                line(firstOrgProductId, "DL-1", "36", "100.0000", "2", 18L),
                line(secondOrgProductId, "DL-2", "5", "7.5000", "5", 1L),
            )
        )

        val inOrder = inOrder(entityAdvisoryLock, stockMovementRepository)
        inOrder.verify(entityAdvisoryLock)
            .acquire(LockNamespaces.PRODUCT, setOf(firstLocationProductId, secondLocationProductId))
        inOrder.verify(stockMovementRepository)
            .findPresentDetailReferences("DISPATCH-1", listOf("DL-1", "DL-2"), MovementType.TRANSFER_IN)
    }
}
