package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsBackfillCandidate
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsBackfillStore
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class LocationSaleLineCogsBackfillerTest {

    private val saleLineCogsBackfillStore = mock(SaleLineCogsBackfillStore::class.java)
    private val referenceNumberGenerator = mock(ReferenceNumberGenerator::class.java)
    private val locationSaleLineCogsBackfiller =
        LocationSaleLineCogsBackfiller(saleLineCogsBackfillStore, referenceNumberGenerator)

    @Test
    fun `a batch hands the missing lines and bulk drawn references to the store`() {
        val firstCandidate = SaleLineCogsBackfillCandidate(UUID.randomUUID(), BigDecimal("12.5000"), OffsetDateTime.parse("2026-10-01T09:30:00Z"))
        val secondCandidate = SaleLineCogsBackfillCandidate(UUID.randomUUID(), BigDecimal.ZERO, null)
        val candidates = listOf(firstCandidate, secondCandidate)
        val referenceNumbers = listOf("SLCG000001", "SLCG000002")
        `when`(saleLineCogsBackfillStore.findLinesMissingCogsRecord(500)).thenReturn(candidates)
        `when`(referenceNumberGenerator.generateBulk(TableName.SALE_LINE_COGS, 2)).thenReturn(referenceNumbers)

        val insertedCount = locationSaleLineCogsBackfiller.backfillNextBatch()

        assertEquals(2, insertedCount)
        verify(saleLineCogsBackfillStore).insertCogsRecords(candidates, referenceNumbers)
    }

    @Test
    fun `an empty batch inserts nothing and draws no references`() {
        `when`(saleLineCogsBackfillStore.findLinesMissingCogsRecord(500)).thenReturn(emptyList())

        val insertedCount = locationSaleLineCogsBackfiller.backfillNextBatch()

        assertEquals(0, insertedCount)
        verify(saleLineCogsBackfillStore, never()).insertCogsRecords(emptyList(), emptyList())
        verify(referenceNumberGenerator, never()).generateBulk(TableName.SALE_LINE_COGS, 1)
    }

    @Test
    fun `counts report what this run inserted next to the two totals that must match`() {
        `when`(saleLineCogsBackfillStore.countSaleLinesWithCogsTotal()).thenReturn(9L)
        `when`(saleLineCogsBackfillStore.countCogsRecords()).thenReturn(9L)

        val counts = locationSaleLineCogsBackfiller.countsAfterInserting(4L)

        assertEquals(4L, counts.insertedCostRecordCount)
        assertEquals(counts.saleLinesWithCogsTotalCount, counts.costRecordCount)
    }
}
