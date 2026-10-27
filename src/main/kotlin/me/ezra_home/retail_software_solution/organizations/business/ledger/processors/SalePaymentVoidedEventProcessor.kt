package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SalePaymentVoidedEventProcessor(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SalePaymentVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SalePaymentVoidedEvent> = SalePaymentVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SalePaymentVoidedEvent): Boolean =
        ledgerPostingGate.isPosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.SALE_PAYMENT_VOID).not()

    override fun prepareLedgerRequest(event: SalePaymentVoidedEvent): LedgerPostingRequest {
        ledgerPostingGate.requirePosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.SALE_PAYMENT)
        val contact = contactService.getContactById(event.contactId)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.paymentReferenceNumber,
            sourceType = LedgerSourceType.SALE_PAYMENT_VOID,
            postingDate = event.voidedOn,
            entries = listOf(
                LedgerEntryRequest.debit(SystemAccount.TRADE_RECEIVABLES.code, PositiveMoney.of(event.amount)),
                LedgerEntryRequest.credit(event.paymentMethodAccountCode, PositiveMoney.of(event.amount))
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = Money.of(event.amount),
                    payableAmount = Money.ZERO
                )
            )
        )
    }
}
