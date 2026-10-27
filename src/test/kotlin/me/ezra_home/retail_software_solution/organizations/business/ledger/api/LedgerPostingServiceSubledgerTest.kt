package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WithDefaultOrganizationSession
class LedgerPostingServiceSubledgerTest {

    private val subledgerEntryRepository = mock(SubledgerEntryRepository::class.java)
    private val subledgerBalanceRepository = mock(SubledgerBalanceRepository::class.java)
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val savedSubledgerEntries = mutableListOf<SubledgerEntryEntity>()

    private val ledgerPostingService = LedgerPostingService(
        ledgerEntryGroupRepository,
        mock(LedgerEntryRepository::class.java),
        subledgerEntryRepository,
        subledgerBalanceRepository,
        mock(AccountsAndLedgerLock::class.java),
        fiscalPeriodService,
        mock(AccountService::class.java)
    )

    private val postingDate = LocalDate.of(2026, 1, 1)
    private val contactReferenceNumber = "CT000001"

    init {
        `when`(fiscalPeriodService.findOpenForDate(postingDate)).thenReturn(UUID.randomUUID())
        `when`(ledgerEntryGroupRepository.save(any(LedgerEntryGroupEntity::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as LedgerEntryGroupEntity).also { it.referenceNumber = "LG000001" }
        }
        `when`(subledgerEntryRepository.saveAll(anyIterable<SubledgerEntryEntity>())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[0] as Iterable<SubledgerEntryEntity>).toList().also { savedSubledgerEntries.addAll(it) }
        }
    }

    @Test
    fun `a posting moves the contact's balance row and stamps the entry with the new totals`() {
        val subledgerBalanceEntity = stubBalance(payable = "100.0000", receivable = "50.0000")

        post(subledgerEntry(payable = "10", receivable = "0"))

        val savedEntry = savedSubledgerEntries.single()
        assertEquals(0, BigDecimal("110").compareTo(subledgerBalanceEntity.runningPayable))
        assertEquals(0, BigDecimal("50").compareTo(subledgerBalanceEntity.runningReceivable))
        assertEquals(0, BigDecimal("110").compareTo(savedEntry.runningPayable))
        assertEquals(0, BigDecimal("50").compareTo(savedEntry.runningReceivable))
    }

    @Test
    fun `two entries for one contact in a single posting accumulate on the same balance row`() {
        val subledgerBalanceEntity = stubBalance(payable = "100.0000", receivable = "50.0000")

        post(subledgerEntry(payable = "10", receivable = "0"), subledgerEntry(payable = "5", receivable = "20"))

        assertEquals(listOf("110", "115"), savedSubledgerEntries.map { it.runningPayable.stripTrailingZeros().toPlainString() })
        assertEquals(listOf("50", "70"), savedSubledgerEntries.map { it.runningReceivable.stripTrailingZeros().toPlainString() })
        assertEquals(0, BigDecimal("115").compareTo(subledgerBalanceEntity.runningPayable))
        assertEquals(0, BigDecimal("70").compareTo(subledgerBalanceEntity.runningReceivable))
    }

    @Test
    fun `a contact without a balance row fails the posting instead of starting from zero`() {
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(setOf(contactReferenceNumber))).thenReturn(emptyList())

        assertThrows(RtsGenericException::class.java) { post(subledgerEntry(payable = "0", receivable = "25")) }

        verify(subledgerEntryRepository, never()).saveAll(anyIterable<SubledgerEntryEntity>())
    }

    @Test
    fun `a posting without subledger entries does not read balances`() {
        post()

        verify(subledgerBalanceRepository, never()).findByContactReferenceNumberIn(setOf(contactReferenceNumber))
        assertEquals(emptyList<SubledgerEntryEntity>(), savedSubledgerEntries)
    }

    private fun stubBalance(payable: String, receivable: String): SubledgerBalanceEntity {
        val subledgerBalanceEntity = SubledgerBalanceEntity(contactReferenceNumber, BigDecimal(payable), BigDecimal(receivable))
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(setOf(contactReferenceNumber)))
            .thenReturn(listOf(subledgerBalanceEntity))
        return subledgerBalanceEntity
    }

    private fun subledgerEntry(payable: String, receivable: String) =
        SubledgerEntryRequest(contactReferenceNumber, payableAmount = Money.of(BigDecimal(payable)), receivableAmount = Money.of(BigDecimal(receivable)))

    private fun post(vararg subledgerEntries: SubledgerEntryRequest) {
        ledgerPostingService.post(
            LedgerPostingRequest(
                sourceReferenceNumber = "SRC-1",
                sourceType = LedgerSourceType.SALE,
                postingDate = postingDate,
                entries = listOf(
                    LedgerEntryRequest.debit("1000", PositiveMoney.of(BigDecimal("10"))),
                    LedgerEntryRequest.credit("4000", PositiveMoney.of(BigDecimal("10")))
                ),
                subledgerEntries = subledgerEntries.toList()
            )
        )
    }
}
