package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SaleConfirmedEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SaleTaxLedgerEntriesBuilder
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SaleVoidedEventProcessor
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType

class SaleCogsLedgerEntriesTest {

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
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val saleConfirmedProcessor = SaleConfirmedEventProcessor(
        contactService, ledgerEntryGroupRepository, saleTaxLedgerEntriesBuilder, ledgerPostingService
    )
    private val saleVoidedProcessor = SaleVoidedEventProcessor(
        contactService, LedgerPostingGate(ledgerEntryGroupRepository), saleTaxLedgerEntriesBuilder, ledgerPostingService
    )
    private val locationLevelContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")

    @BeforeEach
    fun setLocationSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                "SALE-1", LedgerSourceType.SALE, SessionContextProvider.getLocationId()
            )
        ).thenReturn(true)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `confirmed sale debits cost of goods sold and credits inventory in the same balanced group dated at the sale date`() {
        val ledgerPostingRequest = saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent(BigDecimal("40.0000")))

        assertEquals(LocalDate.of(2026, 1, 15), ledgerPostingRequest.postingDate)
        assertEquals(
            listOf(
                LedgerEntryRequest.debit(SystemAccount.COST_OF_GOODS_SOLD.code, BigDecimal("40.0000")),
                LedgerEntryRequest.credit(SystemAccount.INVENTORY.code, BigDecimal("40.0000")),
            ),
            costEntries(ledgerPostingRequest.entries)
        )
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
    }

    @Test
    fun `voided sale debits inventory and credits cost of goods sold dated at the void date`() {
        val ledgerPostingRequest = saleVoidedProcessor.prepareLedgerRequest(saleVoidedEvent(BigDecimal("40.0000")))

        assertEquals(LocalDate.of(2026, 1, 16), ledgerPostingRequest.postingDate)
        assertEquals(
            listOf(
                LedgerEntryRequest.debit(SystemAccount.INVENTORY.code, BigDecimal("40.0000")),
                LedgerEntryRequest.credit(SystemAccount.COST_OF_GOODS_SOLD.code, BigDecimal("40.0000")),
            ),
            costEntries(ledgerPostingRequest.entries)
        )
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
    }

    @Test
    fun `zero cost posts no cost entries on confirm or void`() {
        assertEquals(emptyList<LedgerEntryRequest>(), costEntries(saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent(BigDecimal.ZERO)).entries))
        assertEquals(emptyList<LedgerEntryRequest>(), costEntries(saleVoidedProcessor.prepareLedgerRequest(saleVoidedEvent(BigDecimal.ZERO)).entries))
    }

    @Test
    fun `negative cost is posted so the ledger validator rejects it instead of hiding the bug`() {
        val ledgerPostingRequest = saleConfirmedProcessor.prepareLedgerRequest(saleConfirmedEvent(BigDecimal("-1.0000")))

        assertEquals(2, costEntries(ledgerPostingRequest.entries).size)
        assertThrows(RtsGenericException::class.java) { LedgerEntriesValidator.validate(ledgerPostingRequest.entries) }
    }

    private fun costEntries(entries: List<LedgerEntryRequest>) = entries.filter {
        it.accountCode == SystemAccount.COST_OF_GOODS_SOLD.code || it.accountCode == SystemAccount.INVENTORY.code
    }

    private fun saleConfirmedEvent(cogsTotal: BigDecimal) = SaleConfirmedEvent(
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
        cogsTotal = cogsTotal
    )

    private fun saleVoidedEvent(cogsTotal: BigDecimal) = SaleVoidedEvent(
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
        dateVoided = LocalDate.of(2026, 1, 16),
        cogsTotal = cogsTotal
    )
}
