package me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill

import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentBackfillStore
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCostBackfillCandidate
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class LocationStockAdjustmentBackfillerTest {

    private val stockAdjustmentBackfillStore = mock(StockAdjustmentBackfillStore::class.java)
    private val referenceNumberGenerator = mock(ReferenceNumberGenerator::class.java)
    private val locationStockAdjustmentBackfiller =
        LocationStockAdjustmentBackfiller(stockAdjustmentBackfillStore, referenceNumberGenerator)

    @Test
    fun `a cost batch hands the missing adjustments and bulk drawn references to the store`() {
        val candidates = listOf(
            StockAdjustmentCostBackfillCandidate(UUID.randomUUID(), BigDecimal("12.5000"), null),
            StockAdjustmentCostBackfillCandidate(UUID.randomUUID(), BigDecimal.ZERO, null),
        )
        val referenceNumbers = listOf("SADC000001", "SADC000002")
        `when`(stockAdjustmentBackfillStore.findAdjustmentsMissingCostRecord(500)).thenReturn(candidates)
        `when`(referenceNumberGenerator.generateBulk(TableName.STOCK_ADJUSTMENT_COST, 2)).thenReturn(referenceNumbers)

        val insertedCount = locationStockAdjustmentBackfiller.insertNextBatchOfCostRecords()

        assertEquals(2, insertedCount)
        verify(stockAdjustmentBackfillStore).insertCostRecords(candidates, referenceNumbers)
    }

    @Test
    fun `an empty cost batch inserts nothing and draws no references`() {
        `when`(stockAdjustmentBackfillStore.findAdjustmentsMissingCostRecord(500)).thenReturn(emptyList())

        assertEquals(0, locationStockAdjustmentBackfiller.insertNextBatchOfCostRecords())

        verify(referenceNumberGenerator, never()).generateBulk(TableName.STOCK_ADJUSTMENT_COST, 1)
    }

    @Test
    fun `counts report what this run inserted next to the two totals that must match`() {
        `when`(stockAdjustmentBackfillStore.countAdjustments()).thenReturn(9L)
        `when`(stockAdjustmentBackfillStore.countActiveCostRecords()).thenReturn(9L)

        val counts = locationStockAdjustmentBackfiller.countsAfterInserting(4L)

        assertEquals(4L, counts.insertedCostRecordCount)
        assertEquals(counts.adjustmentCount, counts.activeCostRecordCount)
    }
}
