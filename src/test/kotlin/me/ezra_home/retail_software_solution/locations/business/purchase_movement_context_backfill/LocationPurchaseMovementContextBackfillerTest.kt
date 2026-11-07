package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill

import me.ezra_home.retail_software_solution.locations.business.stock.api.PurchaseMovementContextBackfillStore
import me.ezra_home.retail_software_solution.locations.business.stock.api.PurchaseMovementContextCandidate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class LocationPurchaseMovementContextBackfillerTest {

    private val purchaseMovementContextBackfillStore = mock(PurchaseMovementContextBackfillStore::class.java)
    private val locationPurchaseMovementContextBackfiller =
        LocationPurchaseMovementContextBackfiller(purchaseMovementContextBackfillStore)

    @Test
    fun `a batch hands the movements missing a context to the store`() {
        val candidates = listOf(
            PurchaseMovementContextCandidate(UUID.randomUUID(), "PCH000001"),
            PurchaseMovementContextCandidate(UUID.randomUUID(), "PCH000002")
        )
        val startingId = UUID(0L, 0L)
        `when`(purchaseMovementContextBackfillStore.findMovementsMissingContext(startingId, 500)).thenReturn(candidates)

        val batch = locationPurchaseMovementContextBackfiller.backfillNextBatch(startingId)

        assertEquals(2, batch!!.updatedMovementCount)
        assertEquals(candidates.last().stockMovementId, batch.lastStockMovementId)
        verify(purchaseMovementContextBackfillStore).applyContexts(candidates)
    }

    @Test
    fun `an empty batch updates nothing`() {
        val startingId = UUID(0L, 0L)
        `when`(purchaseMovementContextBackfillStore.findMovementsMissingContext(startingId, 500)).thenReturn(emptyList())

        val batch = locationPurchaseMovementContextBackfiller.backfillNextBatch(startingId)

        assertNull(batch)
        verify(purchaseMovementContextBackfillStore, never()).applyContexts(emptyList())
    }

    @Test
    fun `counts report what this run updated and what is still missing`() {
        `when`(purchaseMovementContextBackfillStore.countMovementsMissingContext()).thenReturn(0L)

        val counts = locationPurchaseMovementContextBackfiller.countsAfterUpdating(4L)

        assertEquals(4L, counts.updatedMovementCount)
        assertTrue(PurchaseMovementContextBackfillResult(UUID.randomUUID(), "Main", counts).isComplete)
    }
}
