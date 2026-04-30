package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SupplierPaymentVoidedEvent
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
class SupplierPaymentVoidAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SupplierPaymentVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SupplierPaymentVoidedEvent> = SupplierPaymentVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SupplierPaymentVoidedEvent): Boolean {
        val paymentWasPosted = ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceType(
            event.paymentReferenceNumber,
            LedgerSourceType.SUPPLIER_PAYMENT
        )
        return paymentWasPosted && ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceType(
            event.paymentReferenceNumber,
            LedgerSourceType.SUPPLIER_PAYMENT_VOID
        ).not()
    }

    override fun prepareLedgerRequest(event: SupplierPaymentVoidedEvent): LedgerPostingRequest {
        val supplier = contactService.getContactById(event.supplierId)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.paymentReferenceNumber,
            sourceType = LedgerSourceType.SUPPLIER_PAYMENT_VOID,
            postingDate = event.voidedOn,
            entries = listOf(
                LedgerEntryRequest(SystemAccount.TRADE_PAYABLES.code, EntryType.CREDIT, event.amount),
                LedgerEntryRequest(event.paymentMethodAccountCode, EntryType.DEBIT, event.amount)
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = supplier.referenceNumber,
                    payableAmount = event.amount,
                    receivableAmount = BigDecimal.ZERO
                )
            )
        )
    }
}
