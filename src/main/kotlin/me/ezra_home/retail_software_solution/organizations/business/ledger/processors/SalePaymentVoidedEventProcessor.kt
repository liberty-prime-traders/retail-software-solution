package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentVoidedEvent
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
class SalePaymentVoidedEventProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SalePaymentVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SalePaymentVoidedEvent> = SalePaymentVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SalePaymentVoidedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
            event.paymentReferenceNumber,
            LedgerSourceType.SALE_PAYMENT_VOID,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: SalePaymentVoidedEvent): LedgerPostingRequest {
        val contact = contactService.getContactById(event.contactId)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.paymentReferenceNumber,
            sourceType = LedgerSourceType.SALE_PAYMENT_VOID,
            postingDate = event.voidedOn,
            entries = listOf(
                LedgerEntryRequest(SystemAccount.TRADE_RECEIVABLES.code, EntryType.DEBIT, event.amount),
                LedgerEntryRequest(event.paymentMethodAccountCode, EntryType.CREDIT, event.amount)
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = event.amount,
                    payableAmount = BigDecimal.ZERO
                )
            )
        )
    }
}
