package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.PurchaseMovementContextBackfillStore
import me.ezra_home.retail_software_solution.locations.business.stock.api.PurchaseMovementContextCandidate
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class PurchaseMovementContextBackfillStoreTest {

    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val purchaseMovementContextBackfillStore = PurchaseMovementContextBackfillStore(stockMovementRepository)

    private fun movement(id: UUID) = StockMovementEntity(
        stockEntryId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        movementType = MovementType.PURCHASE_RECEIVED,
        storageMovedQuantity = BigDecimal.TEN,
        wholeLineQuantityText = "text",
        storageRemainingQuantity = BigDecimal.TEN,
        movedCost = BigDecimal("100.00"),
        storageMovedBreakdownText = "text",
        storageRemainingBreakdownText = "text",
        documentReference = "DLV1",
        detailReference = "DLL1",
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L
    ).also { it.id = id }

    @Test
    fun `each movement gets the context of its own purchase`() {
        val firstMovementId = UUID.randomUUID()
        val secondMovementId = UUID.randomUUID()
        val firstMovement = movement(firstMovementId)
        val secondMovement = movement(secondMovementId)
        `when`(stockMovementRepository.findAllById(anyIterable())).thenReturn(listOf(firstMovement, secondMovement))

        purchaseMovementContextBackfillStore.applyContexts(
            listOf(
                PurchaseMovementContextCandidate(firstMovementId, "PCH000001"),
                PurchaseMovementContextCandidate(secondMovementId, "PCH000002")
            )
        )

        assertEquals("Purchase PCH000001", firstMovement.context)
        assertEquals("Purchase PCH000002", secondMovement.context)
        verify(stockMovementRepository).saveAllAndFlush(listOf(firstMovement, secondMovement))
    }
}
