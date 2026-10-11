package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerMapperImpl
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerAmountTotals
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerBalanceRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryEntity
import me.ezra_home.retail_software_solution.organizations.business.ledger.SubledgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.lock.api.OrgEntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.mappers.StringQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class SubledgerServiceTest {

    private val subledgerEntryRepository = mock(SubledgerEntryRepository::class.java)
    private val subledgerBalanceRepository = mock(SubledgerBalanceRepository::class.java)
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)

    private val subledgerService = SubledgerService(
        subledgerEntryRepository,
        subledgerBalanceRepository,
        ledgerEntryGroupRepository,
        LedgerMapperImpl(StringQualifier),
        orgEntityAdvisoryLock
    )

    private val contactReferenceNumber = "CT000001"

    @Test
    fun `first page over-fetches one row to detect more and hands out the last returned row as cursor`() {
        val newestEntry = entry("LG000003", createdOn = OffsetDateTime.parse("2026-01-03T10:00:00Z"))
        val middleEntry = entry("LG000002", createdOn = OffsetDateTime.parse("2026-01-02T10:00:00Z"))
        val oldestEntry = entry("LG000001", createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"))
        `when`(subledgerEntryRepository.findFirstPage(contactReferenceNumber, 3))
            .thenReturn(listOf(newestEntry, middleEntry, oldestEntry))
        stubGroups("LG000003", "LG000002")

        val pageResponse = subledgerService.getEntries(contactReferenceNumber, "", 2)

        assertTrue(pageResponse.hasMore)
        assertEquals(listOf("LG000003", "LG000002"), pageResponse.contents.map { it.groupReferenceNumber })
        assertEquals(
            KeysetSearchCursor(middleEntry.createdOn!!, middleEntry.id!!).encode(),
            pageResponse.currentCursor
        )
    }

    @Test
    fun `later page continues strictly after the cursor within the same contact`() {
        val cursorCreatedOn = OffsetDateTime.parse("2026-01-02T10:00:00Z")
        val cursorId = UUID.randomUUID()
        val previousCursor = KeysetSearchCursor(cursorCreatedOn, cursorId).encode()
        val oldestEntry = entry("LG000001", createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z"))
        `when`(subledgerEntryRepository.findPageAfter(contactReferenceNumber, cursorCreatedOn, cursorId, 3))
            .thenReturn(listOf(oldestEntry))
        stubGroups("LG000001")

        val pageResponse = subledgerService.getEntries(contactReferenceNumber, previousCursor, 2)

        assertFalse(pageResponse.hasMore)
        assertEquals(1, pageResponse.contents.size)
        val pageEntry = pageResponse.contents.single()
        assertEquals(LedgerSourceType.SALE, pageEntry.sourceType)
        assertEquals(0, BigDecimal("5").compareTo(pageEntry.transactionAmount))
        assertEquals(BalanceSignal.MONEY_IN, pageEntry.transactionBalanceSignal)
        assertEquals(0, BigDecimal("10").compareTo(pageEntry.netBalance))
        assertEquals(BalanceSignal.MONEY_IN, pageEntry.netBalanceSignal)
        verify(subledgerEntryRepository, never()).findFirstPage(contactReferenceNumber, 3)
    }

    @Test
    fun `empty page keeps the incoming cursor so the client does not lose its position`() {
        val cursorCreatedOn = OffsetDateTime.parse("2026-01-01T10:00:00Z")
        val cursorId = UUID.randomUUID()
        val previousCursor = KeysetSearchCursor(cursorCreatedOn, cursorId).encode()
        `when`(subledgerEntryRepository.findPageAfter(contactReferenceNumber, cursorCreatedOn, cursorId, 3))
            .thenReturn(emptyList())

        val pageResponse = subledgerService.getEntries(contactReferenceNumber, previousCursor, 2)

        assertTrue(pageResponse.contents.isEmpty())
        assertEquals(previousCursor, pageResponse.currentCursor)
        verifyNoInteractions(ledgerEntryGroupRepository)
    }

    @Test
    fun `zero balance carries the stored scale`() {
        assertEquals(BigDecimal("0.0000"), SubledgerBalanceDto.ZERO.payable)
        assertEquals(BigDecimal("0.0000"), SubledgerBalanceDto.ZERO.receivable)
    }

    @Test
    fun `page size outside the allowed range is rejected before any query runs`() {
        assertThrows(RtsGenericException::class.java) { subledgerService.getEntries(contactReferenceNumber, "", 0) }
        assertThrows(RtsGenericException::class.java) { subledgerService.getEntries(contactReferenceNumber, "", 201) }
        verifyNoInteractions(subledgerEntryRepository, ledgerEntryGroupRepository)
    }

    @Test
    fun `current balances come from the balance rows and skip the query for no contacts`() {
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(setOf(contactReferenceNumber))).thenReturn(
            listOf(SubledgerBalanceEntity(contactReferenceNumber, BigDecimal("20.0000"), BigDecimal("30.0000")))
        )

        val balancesByContact = subledgerService.getCurrentBalances(setOf(contactReferenceNumber))

        assertEquals(BigDecimal("30.0000"), balancesByContact.getValue(contactReferenceNumber).receivable)
        assertEquals(BigDecimal("20.0000"), balancesByContact.getValue(contactReferenceNumber).payable)
        assertTrue(subledgerService.getCurrentBalances(emptySet()).isEmpty())
        verify(subledgerBalanceRepository, never()).findByContactReferenceNumberIn(emptySet())
    }

    @Test
    fun `reconciling creates a missing row from the entry history and locks the contacts first`() {
        `when`(subledgerEntryRepository.sumAmountsForContacts(setOf(contactReferenceNumber))).thenReturn(
            listOf(StubSubledgerAmountTotals(contactReferenceNumber, BigDecimal("12.5000"), BigDecimal("40.0000")))
        )
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(setOf(contactReferenceNumber))).thenReturn(emptyList())

        subledgerService.reconcileBalances(setOf(contactReferenceNumber))

        val balanceCaptor = ArgumentCaptor.forClass(SubledgerBalanceEntity::class.java)
        verify(subledgerBalanceRepository).save(balanceCaptor.capture())
        assertEquals(0, BigDecimal("12.5").compareTo(balanceCaptor.value.runningPayable))
        assertEquals(0, BigDecimal("40").compareTo(balanceCaptor.value.runningReceivable))
        verify(orgEntityAdvisoryLock).acquire(LockNamespaces.SUBLEDGER_CONTACT, setOf(contactReferenceNumber))
    }

    @Test
    fun `reconciling gives a contact with no entries a zero row`() {
        `when`(subledgerEntryRepository.sumAmountsForContacts(setOf(contactReferenceNumber))).thenReturn(emptyList())
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(setOf(contactReferenceNumber))).thenReturn(emptyList())

        subledgerService.reconcileBalances(setOf(contactReferenceNumber))

        val balanceCaptor = ArgumentCaptor.forClass(SubledgerBalanceEntity::class.java)
        verify(subledgerBalanceRepository).save(balanceCaptor.capture())
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceCaptor.value.runningPayable))
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceCaptor.value.runningReceivable))
    }

    @Test
    fun `reconciling corrects a drifted row to the sum of its entries and leaves a matching row alone`() {
        val driftedContact = "CT000002"
        val driftedBalance = SubledgerBalanceEntity(driftedContact, BigDecimal("1"), BigDecimal("1"))
        val matchingBalance = SubledgerBalanceEntity(contactReferenceNumber, BigDecimal("5.0"), BigDecimal("7.0"))
        val contacts = setOf(contactReferenceNumber, driftedContact)
        `when`(subledgerEntryRepository.sumAmountsForContacts(contacts)).thenReturn(
            listOf(
                StubSubledgerAmountTotals(contactReferenceNumber, BigDecimal("5.0000"), BigDecimal("7.0000")),
                StubSubledgerAmountTotals(driftedContact, BigDecimal("30.0000"), BigDecimal("2.0000"))
            )
        )
        `when`(subledgerBalanceRepository.findByContactReferenceNumberIn(contacts))
            .thenReturn(listOf(matchingBalance, driftedBalance))

        subledgerService.reconcileBalances(contacts)

        assertEquals(0, BigDecimal("30").compareTo(driftedBalance.runningPayable))
        assertEquals(0, BigDecimal("2").compareTo(driftedBalance.runningReceivable))
        assertEquals(BigDecimal("5.0"), matchingBalance.runningPayable)
        verify(subledgerBalanceRepository, never()).save(driftedBalance)
    }

    @Test
    fun `reconciling no contacts touches nothing`() {
        subledgerService.reconcileBalances(emptySet())

        verifyNoInteractions(subledgerEntryRepository, subledgerBalanceRepository, orgEntityAdvisoryLock)
    }

    @Test
    fun `opening a balance saves a zero row for the contact`() {
        subledgerService.openBalance(contactReferenceNumber)

        val balanceCaptor = ArgumentCaptor.forClass(SubledgerBalanceEntity::class.java)
        verify(subledgerBalanceRepository).save(balanceCaptor.capture())
        assertEquals(contactReferenceNumber, balanceCaptor.value.contactReferenceNumber)
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceCaptor.value.runningPayable))
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceCaptor.value.runningReceivable))
    }

    @Test
    fun `net balance is absolute and the signal says who owes whom`() {
        val theyOweUs = SubledgerBalanceDto(payable = BigDecimal("20"), receivable = BigDecimal("50"))
        val weOweThem = SubledgerBalanceDto(payable = BigDecimal("50"), receivable = BigDecimal("20"))
        val settled = SubledgerBalanceDto(payable = BigDecimal("20.0"), receivable = BigDecimal("20.0000"))

        assertEquals(BalanceSignal.MONEY_IN, theyOweUs.balanceSignal)
        assertEquals(0, BigDecimal("30").compareTo(theyOweUs.netBalance))
        assertEquals(BalanceSignal.MONEY_OUT, weOweThem.balanceSignal)
        assertEquals(0, BigDecimal("30").compareTo(weOweThem.netBalance))
        assertEquals(BalanceSignal.ZERO_BALANCE, settled.balanceSignal)
        assertEquals(0, BigDecimal.ZERO.compareTo(settled.netBalance))
    }

    private fun stubGroups(vararg groupReferenceNumbers: String) {
        `when`(ledgerEntryGroupRepository.findByReferenceNumberIn(groupReferenceNumbers.toSet())).thenReturn(
            groupReferenceNumbers.map { groupReferenceNumber ->
                LedgerEntryGroupEntity(
                    sourceReferenceNumber = "SRC-$groupReferenceNumber",
                    sourceType = LedgerSourceType.SALE,
                    fiscalPeriodId = UUID.randomUUID(),
                    postedOn = Instant.parse("2026-01-01T10:00:00Z")
                ).also {
                    it.referenceNumber = groupReferenceNumber
                    it.id = UUID.randomUUID()
                    it.createdById = UUID.randomUUID()
                    it.createdOn = OffsetDateTime.parse("2026-01-01T10:00:00Z")
                }
            }
        )
    }

    private fun entry(groupReferenceNumber: String, createdOn: OffsetDateTime): SubledgerEntryEntity =
        SubledgerEntryEntity(
            groupReferenceNumber = groupReferenceNumber,
            contactReferenceNumber = contactReferenceNumber,
            payableAmount = BigDecimal("5.0000"),
            receivableAmount = BigDecimal("10.0000"),
            runningPayable = BigDecimal("20.0000"),
            runningReceivable = BigDecimal("30.0000")
        ).also {
            it.id = UUID.randomUUID()
            it.createdOn = createdOn
            it.createdById = UUID.randomUUID()
        }
}

private data class StubSubledgerAmountTotals(
    override val contactReferenceNumber: String,
    override val payableTotal: BigDecimal,
    override val receivableTotal: BigDecimal
) : SubledgerAmountTotals
