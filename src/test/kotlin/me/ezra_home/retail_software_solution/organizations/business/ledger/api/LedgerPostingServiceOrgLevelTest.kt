package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

// An org-level posting must never pick up a location: the (reference, source type) idempotency index for null-location groups depends on it.
class LedgerPostingServiceOrgLevelTest {

    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val accountService = mock(AccountService::class.java)
    private val savedGroups = mutableListOf<LedgerEntryGroupEntity>()

    private val ledgerPostingService = LedgerPostingService(
        ledgerEntryGroupRepository,
        mock(LedgerEntryRepository::class.java),
        mock(SubledgerEntryRepository::class.java),
        mock(SubledgerBalanceRepository::class.java),
        mock(AccountsAndLedgerLock::class.java),
        fiscalPeriodService,
        accountService
    )

    private val postingDate = LocalDate.of(2026, 3, 1)

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @BeforeEach
    fun setUpOrganizationSession() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC")))
        `when`(fiscalPeriodService.findOpenForDate(postingDate)).thenReturn(UUID.randomUUID())
        `when`(ledgerEntryGroupRepository.save(any(LedgerEntryGroupEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LedgerEntryGroupEntity).also {
                it.referenceNumber = "LG000001"
                savedGroups.add(it)
            }
        }
    }

    // Only a reversal may build up an inactive account; a plain posting must keep the stricter check.
    @Test
    fun `only a reversal posting is checked with the reversal postability rule`() {
        val entries = listOf(
            LedgerEntryRequest.debit("001.001.001", BigDecimal("10.0000")),
            LedgerEntryRequest.credit("003.001", BigDecimal("10.0000"))
        )

        ledgerPostingService.post(
            LedgerPostingRequest("FMOV000001", LedgerSourceType.FUND_MOVEMENT, postingDate, entries, emptyList())
        )
        verify(accountService).assertPostable(anyList())
        verify(accountService, never()).assertPostableReversal(anyList())

        ledgerPostingService.post(
            LedgerPostingRequest("FMOV000001", LedgerSourceType.FUND_MOVEMENT_VOID, postingDate, entries, emptyList(), reversal = true)
        )
        verify(accountService).assertPostableReversal(anyList())
    }

    @Test
    fun `a fund movement posted with only the organization session has a null source location`() {
        ledgerPostingService.post(
            LedgerPostingRequest(
                sourceReferenceNumber = "FMOV000001",
                sourceType = LedgerSourceType.FUND_MOVEMENT,
                postingDate = postingDate,
                entries = listOf(
                    LedgerEntryRequest.debit("001.001.001", BigDecimal("10.0000")),
                    LedgerEntryRequest.credit("003.001", BigDecimal("10.0000"))
                ),
                subledgerEntries = emptyList()
            )
        )

        val group = savedGroups.single()
        assertNull(group.sourceLocationId)
        assertEquals(LedgerSourceType.FUND_MOVEMENT, group.sourceType)
        assertEquals("FMOV000001", group.sourceReferenceNumber)
    }
}
