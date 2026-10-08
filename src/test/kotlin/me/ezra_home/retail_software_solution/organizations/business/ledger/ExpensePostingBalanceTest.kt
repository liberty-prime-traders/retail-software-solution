package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ExpenseLedgerGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ExpensePaymentRecordedAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ExpensePaymentVoidedAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ExpenseRecordedAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ExpenseVoidedAccountingProcessor
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/** Same approach as [SupplierAndOpeningPostingBalanceTest], for the expense processors. */
class ExpensePostingBalanceTest {

    private val recordingAccountService = RecordingAccountService()
    private val locationId = UUID.randomUUID()
    private val payeeContactId = UUID.randomUUID()
    private val contactService = mock(ContactService::class.java).also { contactService ->
        val payeeDto = mock(ContactDto::class.java)
        `when`(payeeDto.referenceNumber).thenReturn("PAYEE-1")
        `when`(contactService.getContactById(payeeContactId)).thenReturn(payeeDto)
    }
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val expenseLedgerGate = ExpenseLedgerGate(ledgerEntryGroupRepository)

    private val expenseRecordedProcessor = ExpenseRecordedAccountingProcessor(contactService, expenseLedgerGate, ledgerPostingService)
    private val expenseVoidedProcessor = ExpenseVoidedAccountingProcessor(contactService, expenseLedgerGate, ledgerPostingService)
    private val expensePaymentRecordedProcessor = ExpensePaymentRecordedAccountingProcessor(contactService, expenseLedgerGate, ledgerPostingService)
    private val expensePaymentVoidedProcessor = ExpensePaymentVoidedAccountingProcessor(contactService, expenseLedgerGate, ledgerPostingService)

    private val locationLevelContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")
    private val orgLevelContext = EventSourceContext.OrgLevel(orgSchema = "org-a")

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = locationId, schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a wage expense raises wages expense and wages payable`() {
        post(expenseRecordedProcessor.prepareLedgerRequest(expenseRecordedEvent(SystemAccount.WAGES_EXPENSE)))

        assertBalance("100.0000", SystemAccount.WAGES_EXPENSE)
        assertBalance("100.0000", SystemAccount.WAGES_PAYABLE)
        assertBalance("0", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `any other expense account is owed through trade payables`() {
        post(expenseRecordedProcessor.prepareLedgerRequest(expenseRecordedEvent(SystemAccount.RENT_EXPENSE)))

        assertBalance("100.0000", SystemAccount.RENT_EXPENSE)
        assertBalance("100.0000", SystemAccount.TRADE_PAYABLES)
        assertBalance("0", SystemAccount.WAGES_PAYABLE)
    }

    @Test
    fun `settling an expense clears the liability and draws down cash`() {
        post(expenseRecordedProcessor.prepareLedgerRequest(expenseRecordedEvent(SystemAccount.RENT_EXPENSE)))
        post(expensePaymentRecordedProcessor.prepareLedgerRequest(expensePaymentRecordedEvent(SystemAccount.RENT_EXPENSE)))

        assertBalance("100.0000", SystemAccount.RENT_EXPENSE)
        assertBalance("0", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `voiding a settlement restores the liability`() {
        post(expenseRecordedProcessor.prepareLedgerRequest(expenseRecordedEvent(SystemAccount.RENT_EXPENSE)))
        post(expensePaymentRecordedProcessor.prepareLedgerRequest(expensePaymentRecordedEvent(SystemAccount.RENT_EXPENSE)))
        post(expensePaymentVoidedProcessor.prepareLedgerRequest(expensePaymentVoidedEvent(SystemAccount.RENT_EXPENSE)))

        assertBalance("100.0000", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `voiding an expense is the only entry that credits the expense account`() {
        post(expenseRecordedProcessor.prepareLedgerRequest(expenseRecordedEvent(SystemAccount.RENT_EXPENSE)))
        post(expenseVoidedProcessor.prepareLedgerRequest(expenseVoidedEvent(SystemAccount.RENT_EXPENSE)))

        assertBalance("0", SystemAccount.RENT_EXPENSE)
        assertBalance("0", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `incurring raises what is owed to the payee and settling reduces it`() {
        val incurRequest = expenseRecordedProcessor.prepareLedgerRequest(expenseRecordedEvent(SystemAccount.RENT_EXPENSE))
        val settleRequest = expensePaymentRecordedProcessor.prepareLedgerRequest(expensePaymentRecordedEvent(SystemAccount.RENT_EXPENSE))

        assertEquals("PAYEE-1", incurRequest.subledgerEntries.single().contactReferenceNumber)
        assertEquals(0, BigDecimal("100").compareTo(incurRequest.subledgerEntries.single().payableAmount))
        assertEquals(0, BigDecimal.ZERO.compareTo(incurRequest.subledgerEntries.single().receivableAmount))
        assertEquals(0, BigDecimal("100").compareTo(settleRequest.subledgerEntries.single().receivableAmount))
        assertEquals(0, BigDecimal.ZERO.compareTo(settleRequest.subledgerEntries.single().payableAmount))
    }

    @Test
    fun `a void is only processed once the original posting exists`() {
        val voidEvent = expenseVoidedEvent(SystemAccount.RENT_EXPENSE)

        assertFalse(expenseVoidedProcessor.shouldProcess(voidEvent))

        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                "EXPN01", LedgerSourceType.EXPENSE, locationId
            )
        ).thenReturn(true)
        assertTrue(expenseVoidedProcessor.shouldProcess(voidEvent))
    }

    @Test
    fun `an org-level session is keyed without a location`() {
        setOrgOnlySession()

        assertTrue(expenseRecordedProcessor.shouldProcess(expenseRecordedEvent(SystemAccount.RENT_EXPENSE, orgLevelContext)))

        verify(ledgerEntryGroupRepository).existsBySourceReferenceNumberAndSourceTypeAndSourceLocationIdIsNull(
            "EXPN01", LedgerSourceType.EXPENSE
        )
        assertEquals("uq_ledger_entry_grp_ref_type_org", expenseRecordedProcessor.idempotencyConstraintName)
    }

    @Test
    fun `a location-level event outside its location session is rejected rather than keyed to the org`() {
        setOrgOnlySession()

        assertThrows(RtsGenericException::class.java) {
            expenseRecordedProcessor.shouldProcess(expenseRecordedEvent(SystemAccount.RENT_EXPENSE, locationLevelContext))
        }
    }

    @Test
    fun `an org void is only processed once the org posting exists`() {
        setOrgOnlySession()
        val voidEvent = expenseVoidedEvent(SystemAccount.RENT_EXPENSE, orgLevelContext)

        assertFalse(expenseVoidedProcessor.shouldProcess(voidEvent))

        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationIdIsNull(
                "EXPN01", LedgerSourceType.EXPENSE
            )
        ).thenReturn(true)
        assertTrue(expenseVoidedProcessor.shouldProcess(voidEvent))
    }

    private fun setOrgOnlySession() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
        val accountPostings = ledgerPostingRequest.entries.map {
            AccountPosting(accountCode = it.accountCode, amount = it.amount, entryType = it.entryType)
        }
        recordingAccountService.accountService.assertPostable(accountPostings)
        recordingAccountService.accountService.patchBalances(accountPostings)
    }

    private fun assertBalance(expected: String, systemAccount: SystemAccount) {
        assertEquals(
            0,
            BigDecimal(expected).compareTo(recordingAccountService.balance(systemAccount)),
            "${systemAccount.name} expected $expected but was ${recordingAccountService.balance(systemAccount)}"
        )
    }

    private fun expenseRecordedEvent(expenseAccount: SystemAccount, sourceContext: EventSourceContext = locationLevelContext) = ExpenseRecordedEvent(
        eventId = UUID.randomUUID(), sourceContext = sourceContext, timestamp = Instant.now(), correlationId = null,
        expenseId = UUID.randomUUID(), expenseReferenceNumber = "EXPN01", expenseAccountCode = expenseAccount.code,
        payeeContactId = payeeContactId, amount = BigDecimal("100"), expenseDate = LocalDate.of(2026, 1, 15)
    )

    private fun expenseVoidedEvent(expenseAccount: SystemAccount, sourceContext: EventSourceContext = locationLevelContext) = ExpenseVoidedEvent(
        eventId = UUID.randomUUID(), sourceContext = sourceContext, timestamp = Instant.now(), correlationId = null,
        voidId = UUID.randomUUID(), expenseId = UUID.randomUUID(), expenseReferenceNumber = "EXPN01",
        expenseAccountCode = expenseAccount.code, payeeContactId = payeeContactId, amount = BigDecimal("100"),
        voidedOn = LocalDate.of(2026, 1, 16)
    )

    private fun expensePaymentRecordedEvent(expenseAccount: SystemAccount) = ExpensePaymentRecordedEvent(
        eventId = UUID.randomUUID(), sourceContext = locationLevelContext, timestamp = Instant.now(), correlationId = null,
        paymentId = UUID.randomUUID(), paymentReferenceNumber = "EXPY01", expenseAccountCode = expenseAccount.code,
        payeeContactId = payeeContactId, paymentMethodAccountCode = SystemAccount.CASH.code, amount = BigDecimal("100"),
        paymentDate = OffsetDateTime.parse("2026-01-15T00:00:00Z")
    )

    private fun expensePaymentVoidedEvent(expenseAccount: SystemAccount) = ExpensePaymentVoidedEvent(
        eventId = UUID.randomUUID(), sourceContext = locationLevelContext, timestamp = Instant.now(), correlationId = null,
        voidId = UUID.randomUUID(), paymentId = UUID.randomUUID(), paymentReferenceNumber = "EXPY01",
        expenseAccountCode = expenseAccount.code, payeeContactId = payeeContactId,
        paymentMethodAccountCode = SystemAccount.CASH.code, amount = BigDecimal("100"), voidedOn = LocalDate.of(2026, 1, 16)
    )
}
