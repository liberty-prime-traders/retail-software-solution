package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class NetStockDrawReaderTest {

    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val netStockDrawReader = NetStockDrawReader(stockMovementRepository, entityAdvisoryLock)
    private val locationProductId = UUID.randomUUID()
    private val movementTypes = MovementType.SALE.withReversalTypes()

    @Test
    fun `the product lock is taken before the movements are read`() {
        `when`(stockMovementRepository.findLocationProductIdsOfDocument("SALE-1", MovementType.SALE))
            .thenReturn(setOf(locationProductId))
        `when`(stockMovementRepository.findByDocumentReferenceAndMovementTypeInOrderByCreatedOn("SALE-1", movementTypes))
            .thenReturn(emptyList())

        netStockDrawReader.lockAndReadNetDrawsOfDocument("SALE-1", MovementType.SALE)

        val lockThenRead = inOrder(entityAdvisoryLock, stockMovementRepository)
        lockThenRead.verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        lockThenRead.verify(stockMovementRepository)
            .findByDocumentReferenceAndMovementTypeInOrderByCreatedOn("SALE-1", movementTypes)
    }

    @Test
    fun `a document with no draws takes no lock and reads no movements`() {
        `when`(stockMovementRepository.findLocationProductIdsOfDocument("SALE-1", MovementType.SALE))
            .thenReturn(emptySet())

        assertEquals(emptyList<NetStockDraw>(), netStockDrawReader.lockAndReadNetDrawsOfDocument("SALE-1", MovementType.SALE))

        verifyNoInteractions(entityAdvisoryLock)
        verify(stockMovementRepository, never())
            .findByDocumentReferenceAndMovementTypeInOrderByCreatedOn("SALE-1", movementTypes)
    }

    @Test
    fun `only the requested lines are netted`() {
        val transferTypes = MovementType.TRANSFER_OUT.withReversalTypes()
        val layerId = UUID.randomUUID()
        `when`(stockMovementRepository.findLocationProductIdsOfDocumentLines(
            "DISP-1", listOf("LINE-1"), MovementType.TRANSFER_OUT
        )).thenReturn(setOf(locationProductId))
        `when`(stockMovementRepository.findByDocumentReferenceAndMovementTypeInOrderByCreatedOn("DISP-1", transferTypes))
            .thenReturn(listOf(
                transferOut(layerId, "LINE-1", "2"),
                transferOut(layerId, "LINE-2", "3"),
            ))

        val netStockDraws = netStockDrawReader.lockAndReadNetDrawsOfLines(
            "DISP-1", listOf("LINE-1"), MovementType.TRANSFER_OUT
        )

        assertEquals(listOf("LINE-1"), netStockDraws.map { it.detailReference })
    }

    private fun transferOut(stockEntryId: UUID, detailReference: String, quantity: String) = StockMovementEntity(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = MovementType.TRANSFER_OUT,
        storageMovedQuantity = BigDecimal(quantity),
        wholeLineQuantityText = "$quantity Pieces",
        storageRemainingQuantity = BigDecimal.ZERO,
        movedCost = BigDecimal("1.00"),
        storageMovedBreakdownText = "$quantity Pieces",
        storageRemainingBreakdownText = "0 Pieces",
        documentReference = "DISP-1",
        detailReference = detailReference,
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
    )
}
