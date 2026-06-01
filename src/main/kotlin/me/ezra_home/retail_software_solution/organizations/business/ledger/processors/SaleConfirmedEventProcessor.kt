package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
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
class SaleConfirmedEventProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    private val saleTaxLedgerEntriesBuilder: SaleTaxLedgerEntriesBuilder,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SaleConfirmedEvent>(ledgerPostingService) {

    override val eventType: KClass<SaleConfirmedEvent> = SaleConfirmedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SaleConfirmedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(event.saleReferenceNumber, SessionContextProvider.getLocationId()).not()

    override fun prepareLedgerRequest(event: SaleConfirmedEvent): LedgerPostingRequest {
        val contact = contactService.getContactById(event.contactId)
        val payableTotal = event.payableTotal
        val grossRevenue = payableTotal.add(event.discountTotal)

        val ledgerEntries = buildList {
            addAll(saleTaxLedgerEntriesBuilder.buildTransactionLevelEntries(event.dateSold, payableTotal))
            add(LedgerEntryRequest(SystemAccount.TRADE_RECEIVABLES.code, EntryType.DEBIT, payableTotal))
            add(LedgerEntryRequest(SystemAccount.GROSS_SALES.code, EntryType.CREDIT, grossRevenue))
            if (event.discountTotal.signum() > 0) {
                add(LedgerEntryRequest(SystemAccount.SALES_DISCOUNTS.code, EntryType.DEBIT, event.discountTotal))
            }
        }

        return LedgerPostingRequest(
            sourceReferenceNumber = event.saleReferenceNumber,
            sourceType = LedgerSourceType.SALE,
            postingDate = event.dateSold,
            entries = ledgerEntries,
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = payableTotal,
                    payableAmount = BigDecimal.ZERO
                )
            )
        )
    }

}
