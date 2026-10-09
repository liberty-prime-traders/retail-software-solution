package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SaleConfirmedEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SalePaymentRecordedEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SalePaymentVoidedEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SaleTaxLedgerEntriesBuilder
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SaleVoidedEventProcessor
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Runs real processor output through the real AccountService.patchBalances and asserts each
 * account's stored balance — which is always in that account's own normal direction.
 */
class LedgerPostingBalanceTest {

    private val recordingAccountService = RecordingAccountService()
    private val accountService = recordingAccountService.accountService
    private val contactId = UUID.randomUUID()
    private val contactService = mock(ContactService::class.java).also { contactService ->
        val contactDto = mock(ContactDto::class.java)
        `when`(contactDto.referenceNumber).thenReturn("CONTACT-1")
        `when`(contactService.getContactById(contactId)).thenReturn(contactDto)
    }
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val saleTaxLedgerEntriesBuilder = mock(SaleTaxLedgerEntriesBuilder::class.java).also { builder ->
        `when`(builder.buildTransactionLevelEntries("SALE-1")).thenReturn(emptyList())
        `when`(builder.buildTransactionLevelReversalEntries("SALE-1")).thenReturn(emptyList())
    }
    private val ledgerPostingGate = LedgerPostingGate(ledgerEntryGroupRepository)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)

    private val saleConfirmedProcessor =
        SaleConfirmedEventProcessor(contactService, ledgerEntryGroupRepository, saleTaxLedgerEntriesBuilder, ledgerPostingService)
    private val saleVoidedProcessor =
        SaleVoidedEventProcessor(contactService, ledgerPostingGate, saleTaxLedgerEntriesBuilder, ledgerPostingService)
    private val salePaymentRecordedProcessor =
        SalePaymentRecordedEventProcessor(contactService, ledgerEntryGroupRepository, ledgerPostingService)
    private val salePaymentVoidedProcessor =
        SalePaymentVoidedEventProcessor(contactService, ledgerPostingGate, ledgerPostingService)

    @BeforeEach
    fun setLocationSession() {
        SessionContextProvider.setSession(SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            ))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `confirming a discounted sale leaves every account positive in its own normal direction`() {
        post(saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent()))

        assertEquals(0, BigDecimal("90.00").compareTo(balance(SystemAccount.TRADE_RECEIVABLES)))
        assertEquals(0, BigDecimal("100.00").compareTo(balance(SystemAccount.GROSS_SALES)))
        assertEquals(0, BigDecimal("10.00").compareTo(balance(SystemAccount.SALES_DISCOUNTS)))
    }

    @Test
    fun `voiding a sale returns every account to zero`() {
        post(saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent()))
        markPosted("SALE-1", LedgerSourceType.SALE)
        post(saleVoidedProcessor.prepareLedgerRequest(saleVoidedEvent()))

        listOf(SystemAccount.TRADE_RECEIVABLES, SystemAccount.GROSS_SALES, SystemAccount.SALES_DISCOUNTS).forEach {
            assertEquals(0, BigDecimal.ZERO.compareTo(balance(it)), "${it.name} should be back to zero")
        }
    }

    @Test
    fun `recording a payment moves cash up and receivables down`() {
        post(saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent()))
        val paymentRequest = salePaymentRecordedProcessor.prepareLedgerRequests(salePaymentRecordedEvent(BigDecimal("40.00"))).single()
        post(paymentRequest)

        assertEquals(0, BigDecimal("40.00").compareTo(balance(SystemAccount.CASH)))
        assertEquals(0, BigDecimal("50.00").compareTo(balance(SystemAccount.TRADE_RECEIVABLES)))
    }

    @Test
    fun `voiding a payment puts cash back down and receivables back up`() {
        post(saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent()))
        post(salePaymentRecordedProcessor.prepareLedgerRequests(salePaymentRecordedEvent(BigDecimal("40.00"))).single())
        markPosted("PAY-1", LedgerSourceType.SALE_PAYMENT)
        post(salePaymentVoidedProcessor.prepareLedgerRequest(salePaymentVoidedEvent(BigDecimal("40.00"))))

        assertEquals(0, BigDecimal.ZERO.compareTo(balance(SystemAccount.CASH)))
        assertEquals(0, BigDecimal("90.00").compareTo(balance(SystemAccount.TRADE_RECEIVABLES)))
    }

    @Test
    fun `a sale void is skipped once the void is posted and fails while the sale itself is unposted`() {
        assertTrue(saleVoidedProcessor.shouldProcess(saleVoidedEvent()))
        assertThrows(RtsGenericException::class.java) { saleVoidedProcessor.prepareLedgerRequest(saleVoidedEvent()) }

        markPosted("SALE-1", LedgerSourceType.SALE_VOID)
        assertFalse(saleVoidedProcessor.shouldProcess(saleVoidedEvent()))
    }

    @Test
    fun `a payment void is skipped once the void is posted and fails while the payment itself is unposted`() {
        assertTrue(salePaymentVoidedProcessor.shouldProcess(salePaymentVoidedEvent(BigDecimal("40.00"))))
        assertThrows(RtsGenericException::class.java) {
            salePaymentVoidedProcessor.prepareLedgerRequest(salePaymentVoidedEvent(BigDecimal("40.00")))
        }

        markPosted("PAY-1", LedgerSourceType.SALE_PAYMENT_VOID)
        assertFalse(salePaymentVoidedProcessor.shouldProcess(salePaymentVoidedEvent(BigDecimal("40.00"))))
    }

    private fun markPosted(reference: String, sourceType: LedgerSourceType) {
        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                reference, sourceType, SessionContextProvider.getLocationId()
            )
        ).thenReturn(true)
    }

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
        val accountPostings = ledgerPostingRequest.entries.map {
            AccountPosting(accountCode = it.accountCode, amount = it.amount, entryType = it.entryType)
        }
        accountService.assertPostable(accountPostings)
        accountService.patchBalances(accountPostings)
    }

    private fun balance(systemAccount: SystemAccount) = recordingAccountService.balance(systemAccount)

    private val locationLevelContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")

    private fun saleConfirmedEvent() = SaleConfirmedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        contactId = contactId,
        saleReferenceNumber = "SALE-1",
        taxableAmount = BigDecimal("90.00"),
        taxBilled = BigDecimal.ZERO,
        discountTotal = BigDecimal("10.00"),
        dateSold = LocalDate.of(2026, 1, 15)
    )

    private fun saleVoidedEvent() = SaleVoidedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        contactId = contactId,
        saleReferenceNumber = "SALE-1",
        taxableAmount = BigDecimal("90.00"),
        taxBilled = BigDecimal.ZERO,
        discountTotal = BigDecimal("10.00"),
        dateSold = LocalDate.of(2026, 1, 15),
        dateVoided = LocalDate.of(2026, 1, 16)
    )

    private fun salePaymentRecordedEvent(amount: BigDecimal) = SalePaymentRecordedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        contactId = contactId,
        payments = listOf(
            SalePaymentLineDto(
                paymentReferenceNumber = "PAY-1",
                paymentMethodAccountCode = SystemAccount.CASH.code,
                amount = amount,
                paymentDate = OffsetDateTime.now()
            )
        )
    )

    private fun salePaymentVoidedEvent(amount: BigDecimal) = SalePaymentVoidedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        contactId = contactId,
        paymentReferenceNumber = "PAY-1",
        paymentMethodAccountCode = SystemAccount.CASH.code,
        amount = amount,
        voidedOn = LocalDate.of(2026, 1, 17)
    )
}
