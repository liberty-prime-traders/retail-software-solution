package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class LedgerMovementReaderTest {

    private val ledgerMovementReader = LedgerMovementReader(mock(LedgerEntryRepository::class.java))

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `day starts follow a fixed-offset organization zone so a 21 30 UTC posting lands on the next local date`() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(UUID.randomUUID(), "org-a", "+03:00")))

        val dayStarts = ledgerMovementReader.dayStartsFor(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 10))

        assertEquals(
            listOf(Instant.parse("2026-03-07T21:00:00Z"), Instant.parse("2026-03-08T21:00:00Z"), Instant.parse("2026-03-09T21:00:00Z")),
            dayStarts
        )
        val postedOn = Instant.parse("2026-03-08T21:30:00Z")
        assertEquals(2, dayStarts.count { dayStart -> !dayStart.isAfter(postedOn) })
    }

    @Test
    fun `every movement source maps to a ledger source type of the same name`() {
        LedgerMovementSource.entries.forEach { movementSource -> LedgerSourceType.valueOf(movementSource.name) }
    }
}
