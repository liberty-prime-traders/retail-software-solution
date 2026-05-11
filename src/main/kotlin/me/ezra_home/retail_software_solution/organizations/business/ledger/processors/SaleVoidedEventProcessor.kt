package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.reflect.KClass

@Service
class SaleVoidedEventProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    private val saleTaxLedgerEntriesBuilder: SaleTaxLedgerEntriesBuilder,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SaleVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SaleVoidedEvent> = SaleVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SaleVoidedEvent): Boolean {
        val locationId = SessionContextProvider.getLocationId()
        val saleWasPosted = ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
            event.saleReferenceNumber, LedgerSourceType.SALE, locationId
        )
        if (!saleWasPosted) return false
        val alreadyReversed = ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
            event.saleReferenceNumber, LedgerSourceType.SALE_VOID, locationId
        )
        return !alreadyReversed
    }

    override fun prepareLedgerRequest(event: SaleVoidedEvent): LedgerPostingRequest {
        val contact = contactService.getContactById(event.contactId)
        val netAmount = event.subtotal - event.discountTotal

        val taxEntries = saleTaxLedgerEntriesBuilder.buildTransactionLevelReversalEntries(event.dateSold, netAmount)
        val saleEntries = listOf(
            LedgerEntryRequest(SystemAccount.TRADE_RECEIVABLES.code, EntryType.CREDIT, netAmount),
            LedgerEntryRequest(SystemAccount.GROSS_SALES.code, EntryType.DEBIT, netAmount)
        )

        return LedgerPostingRequest(
            sourceReferenceNumber = event.saleReferenceNumber,
            sourceType = LedgerSourceType.SALE_VOID,
            postingDate = event.dateVoided,
            entries = saleEntries + taxEntries,
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = BigDecimal.ZERO,
                    payableAmount = netAmount
                )
            )
        )
    }
}
