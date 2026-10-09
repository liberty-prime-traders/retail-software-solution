package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.OpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.OpeningStockDeclaredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SupplierPaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SupplierPaymentVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.RecordingAccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.OpeningBalanceAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.OpeningStockAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.PurchaseDeliveryAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SupplierPaymentAccountingProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.SupplierPaymentVoidAccountingProcessor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/** Same approach as [LedgerPostingBalanceTest], for the purchase, supplier payment and opening processors. */
class SupplierAndOpeningPostingBalanceTest {

    private val recordingAccountService = RecordingAccountService()
    private val supplierId = UUID.randomUUID()
    private val contactService = mock(ContactService::class.java).also { contactService ->
        val supplierDto = mock(ContactDto::class.java)
        `when`(supplierDto.referenceNumber).thenReturn("SUPPLIER-1")
        `when`(contactService.getContactById(supplierId)).thenReturn(supplierDto)
    }
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)

    private val purchaseDeliveryProcessor = PurchaseDeliveryAccountingProcessor(contactService, ledgerEntryGroupRepository, ledgerPostingService)
    private val supplierPaymentProcessor = SupplierPaymentAccountingProcessor(contactService, ledgerEntryGroupRepository, ledgerPostingService)
    private val supplierPaymentVoidProcessor = SupplierPaymentVoidAccountingProcessor(contactService, LedgerPostingGate(ledgerEntryGroupRepository), ledgerPostingService)
    private val openingStockProcessor = OpeningStockAccountingProcessor(ledgerEntryGroupRepository, ledgerPostingService)
    private val openingBalanceProcessor = OpeningBalanceAccountingProcessor(ledgerEntryGroupRepository, ledgerPostingService)

    private val locationLevelContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")
    private val locationId = UUID.randomUUID()

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
    fun `a delivery raises inventory and trade payables`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))

        assertBalance("10.00", SystemAccount.INVENTORY)
        assertBalance("10.00", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `paying a supplier lowers cash and trade payables`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))
        post(supplierPaymentProcessor.prepareLedgerRequest(supplierPaymentRecordedEvent("4.00")))

        assertBalance("-4.00", SystemAccount.CASH)
        assertBalance("6.00", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `voiding a supplier payment restores cash and trade payables`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))
        post(supplierPaymentProcessor.prepareLedgerRequest(supplierPaymentRecordedEvent("4.00")))
        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                "SPAY-1", LedgerSourceType.SUPPLIER_PAYMENT, locationId
            )
        ).thenReturn(true)
        post(supplierPaymentVoidProcessor.prepareLedgerRequest(supplierPaymentVoidedEvent("4.00")))

        assertBalance("0.00", SystemAccount.CASH)
        assertBalance("10.00", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `opening stock raises inventory and opening balance equity`() {
        post(openingStockProcessor.prepareLedgerRequest(openingStockDeclaredEvent()))

        assertBalance("6.00", SystemAccount.INVENTORY)
        assertBalance("6.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a debit opening balance on a debit normal account raises both sides`() {
        post(openingBalanceProcessor.prepareLedgerRequest(openingBalanceUpsertedEvent(SystemAccount.CASH, EntryType.DEBIT, "50.00")))

        assertBalance("50.00", SystemAccount.CASH)
        assertBalance("50.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    @Test
    fun `a credit opening balance on a credit normal account raises it and draws equity down`() {
        post(openingBalanceProcessor.prepareLedgerRequest(openingBalanceUpsertedEvent(SystemAccount.TRADE_PAYABLES, EntryType.CREDIT, "30.00")))

        assertBalance("30.00", SystemAccount.TRADE_PAYABLES)
        assertBalance("-30.00", SystemAccount.OPENING_BALANCE_EQUITY)
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

    private fun purchaseDeliveredEvent() = PurchaseDeliveredEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        purchaseId = UUID.randomUUID(),
        deliveryId = UUID.randomUUID(),
        deliveryReferenceNumber = "DEL-1",
        deliveredAt = OffsetDateTime.now(),
        supplierId = supplierId,
        lines = listOf(
            PurchaseDeliveredLineDto(
                deliveryLineId = UUID.randomUUID(),
                lineReferenceNumber = "DEL-1-1",
                locationProductId = UUID.randomUUID(),
                quantityDelivered = BigDecimal("2"),
                unitId = UUID.randomUUID(),
                unitCost = BigDecimal("5.00")
            )
        )
    )

    private fun supplierPaymentRecordedEvent(amount: String) = SupplierPaymentRecordedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        paymentId = UUID.randomUUID(),
        supplierId = supplierId,
        paymentMethodAccountCode = SystemAccount.CASH.code,
        amount = BigDecimal(amount),
        paymentDate = OffsetDateTime.now(),
        paymentReferenceNumber = "SPAY-1"
    )

    private fun supplierPaymentVoidedEvent(amount: String) = SupplierPaymentVoidedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        voidId = UUID.randomUUID(),
        paymentId = UUID.randomUUID(),
        supplierId = supplierId,
        paymentMethodAccountCode = SystemAccount.CASH.code,
        amount = BigDecimal(amount),
        voidedOn = LocalDate.of(2026, 1, 20),
        paymentReferenceNumber = "SPAY-1"
    )

    private fun openingStockDeclaredEvent() = OpeningStockDeclaredEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        openingStockId = UUID.randomUUID(),
        ledgerSourceReferenceNumber = "OS-1",
        locationProductId = UUID.randomUUID(),
        quantity = BigDecimal("3"),
        unitCost = BigDecimal("2.00"),
        postingDate = LocalDate.of(2026, 1, 1)
    )

    private fun openingBalanceUpsertedEvent(systemAccount: SystemAccount, entryType: EntryType, amount: String) =
        OpeningBalanceUpsertedEvent(
            eventId = UUID.randomUUID(),
            sourceContext = EventSourceContext.OrgLevel(orgSchema = "org-a"),
            timestamp = Instant.now(),
            correlationId = null,
            openingBalanceId = UUID.randomUUID(),
            ledgerSourceReferenceNumber = "OB-1",
            accountCode = systemAccount.code,
            accountEntryType = entryType,
            amount = BigDecimal(amount),
            postingDate = LocalDate.of(2026, 1, 1)
        )
}
