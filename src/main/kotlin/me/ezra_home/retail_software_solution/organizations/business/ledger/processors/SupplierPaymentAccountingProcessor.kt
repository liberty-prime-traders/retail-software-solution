package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SupplierPaymentRecordedEvent
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
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.reflect.KClass

@Service
class SupplierPaymentAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SupplierPaymentRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<SupplierPaymentRecordedEvent> = SupplierPaymentRecordedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SupplierPaymentRecordedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.paymentReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: SupplierPaymentRecordedEvent): LedgerPostingRequest {
        val supplier = contactService.getContactById(event.supplierId)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.paymentReferenceNumber,
            sourceType = LedgerSourceType.SUPPLIER_PAYMENT,
            postingDate = DateTimes.Local.atOrganizationZone(event.paymentDate),
            entries = listOf(
                LedgerEntryRequest(event.paymentMethodAccountCode, EntryType.CREDIT, event.amount),
                LedgerEntryRequest(SystemAccount.TRADE_PAYABLES.code, EntryType.DEBIT, event.amount)
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = supplier.referenceNumber,
                    payableAmount = BigDecimal.ZERO,
                    receivableAmount = event.amount
                )
            )
        )
    }
}
