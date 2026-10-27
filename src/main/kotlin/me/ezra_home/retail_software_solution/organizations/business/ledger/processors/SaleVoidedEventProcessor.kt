package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.stereotype.Service
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

        val taxEntries = saleTaxLedgerEntriesBuilder.buildTransactionLevelReversalEntries(event.saleReferenceNumber)
        val saleEntries = buildList {
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.TRADE_RECEIVABLES.code, event.amountOwed.toBigDecimal()))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.GROSS_SALES.code, event.grossRevenue.toBigDecimal()))
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.SALES_DISCOUNTS.code, event.discountTotal.toBigDecimal()))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, event.cogsTotal.toBigDecimal()))
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, event.cogsTotal.toBigDecimal()))
        }

        return LedgerPostingRequest(
            sourceReferenceNumber = event.saleReferenceNumber,
            sourceType = LedgerSourceType.SALE_VOID,
            postingDate = event.dateVoided,
            entries = saleEntries + taxEntries,
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = Money.ZERO,
                    payableAmount = event.amountOwed
                )
            )
        )
    }
}
