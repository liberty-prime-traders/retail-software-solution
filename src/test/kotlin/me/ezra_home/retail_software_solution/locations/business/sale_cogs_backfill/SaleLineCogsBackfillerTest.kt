package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill.api.SaleLineCogsBackfiller
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class SaleLineCogsBackfillerTest {

    private val locationService = mock(LocationService::class.java)
    private val locationSaleLineCogsBackfiller = mock(LocationSaleLineCogsBackfiller::class.java)
    private val inlineAsyncExecutor = object : AsyncExecutor {
        override fun execute(block: () -> Unit) = block()
    }
    private val saleLineCogsBackfiller = SaleLineCogsBackfiller(locationService, locationSaleLineCogsBackfiller, inlineAsyncExecutor)

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a failing location does not stop the remaining locations, which loop batches until none are left`() {
        `when`(locationService.getAllLocationDtos()).thenReturn(listOf(location("Main Store", "loc-1"), location("Warehouse", "loc-2")))
        `when`(locationSaleLineCogsBackfiller.backfillNextBatch())
            .thenThrow(IllegalStateException("boom"))
            .thenReturn(2)
            .thenReturn(0)
        `when`(locationSaleLineCogsBackfiller.countsAfterInserting(2L)).thenReturn(SaleLineCogsBackfillCounts(2, 2, 2))

        saleLineCogsBackfiller.startBackfillAllLocations()

        verify(locationSaleLineCogsBackfiller, times(3)).backfillNextBatch()
        verify(locationSaleLineCogsBackfiller).countsAfterInserting(2L)
    }

    @Test
    fun `summary names every outcome and flags counts that differ`() {
        val reconciled = SaleLineCogsBackfillResult(UUID.randomUUID(), "Main Store", SaleLineCogsBackfillCounts(5, 10, 10))
        val mismatched = SaleLineCogsBackfillResult(UUID.randomUUID(), "Warehouse", SaleLineCogsBackfillCounts(1, 10, 9))

        val summary = SaleLineCogsBackfillLogFormatter.formatSummary(listOf(reconciled, mismatched), listOf("Kisumu"))

        assertTrue(summary.contains("3 location(s): 1 reconciled, 1 mismatched, 1 failed"))
        assertTrue(summary.contains("MISMATCH  Warehouse"))
        assertTrue(summary.contains("FAILED    Kisumu"))
        assertEquals(false, mismatched.isReconciled)
    }

    private fun location(name: String, schemaName: String) = LocationDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
        referenceNumber = "LOC000001",
        name = name,
        schemaName = schemaName,
        timezone = "UTC",
    )
}
