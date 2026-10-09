package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
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
    private val ledgerPostingGate: LedgerPostingGate,
    private val saleTaxLedgerEntriesBuilder: SaleTaxLedgerEntriesBuilder,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SaleVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SaleVoidedEvent> = SaleVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SaleVoidedEvent): Boolean =
        ledgerPostingGate.isPosted(event.sourceContext, event.saleReferenceNumber, LedgerSourceType.SALE_VOID).not()

    override fun prepareLedgerRequest(event: SaleVoidedEvent): LedgerPostingRequest {
        ledgerPostingGate.requirePosted(event.sourceContext, event.saleReferenceNumber, LedgerSourceType.SALE)
        val contact = contactService.getContactById(event.contactId)
        val taxableAmount = event.taxableAmount
        val amountOwed = taxableAmount.add(event.taxBilled)
        val grossRevenue = taxableAmount.add(event.discountTotal)

        val taxEntries = saleTaxLedgerEntriesBuilder.buildTransactionLevelReversalEntries(event.saleReferenceNumber)
        val saleEntries = buildList {
            add(LedgerEntryRequest.credit(SystemAccount.TRADE_RECEIVABLES.code, amountOwed))
            add(LedgerEntryRequest.debit(SystemAccount.GROSS_SALES.code, grossRevenue))
            if (event.discountTotal.signum() > 0) {
                add(LedgerEntryRequest.credit(SystemAccount.SALES_DISCOUNTS.code, event.discountTotal))
            }
        }

        return LedgerPostingRequest(
            sourceReferenceNumber = event.saleReferenceNumber,
            sourceType = LedgerSourceType.SALE_VOID,
            postingDate = event.dateVoided,
            entries = saleEntries + taxEntries,
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = BigDecimal.ZERO,
                    payableAmount = amountOwed
                )
            )
        )
    }
}
