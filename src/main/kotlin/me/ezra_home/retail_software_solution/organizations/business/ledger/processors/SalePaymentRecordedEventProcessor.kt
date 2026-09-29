package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentRecordedEvent
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
class SalePaymentRecordedEventProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SalePaymentRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<SalePaymentRecordedEvent> = SalePaymentRecordedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SalePaymentRecordedEvent): Boolean {
        val processedPayments = getProcessedPayments(event.payments)
        return processedPayments.size != event.payments.size
    }

    private fun getProcessedPayments(payments: List<SalePaymentLineDto>): Set<String> {
        val locationId = SessionContextProvider.getLocationId()
        return payments
            .filter { ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(it.paymentReferenceNumber, locationId) }
            .map { it.paymentReferenceNumber }
            .toSet()
    }

    override fun prepareLedgerRequests(event: SalePaymentRecordedEvent): List<LedgerPostingRequest> {
        val contactReferenceNumber = contactService.getContactById(event.contactId).referenceNumber
        val processedPayments = getProcessedPayments(event.payments)
        return event.payments
            .filter { it.paymentReferenceNumber !in processedPayments }
            .map { buildRequest(it, contactReferenceNumber) }
    }

    private fun buildRequest(payment: SalePaymentLineDto, contactReferenceNumber: String) =
        LedgerPostingRequest(
            sourceReferenceNumber = payment.paymentReferenceNumber,
            sourceType = LedgerSourceType.SALE_PAYMENT,
            postingDate = DateTimes.Local.atOrganizationZone(payment.paymentDate),
            entries = listOf(
                LedgerEntryRequest(payment.paymentMethodAccountCode, EntryType.DEBIT, payment.amount),
                LedgerEntryRequest(SystemAccount.TRADE_RECEIVABLES.code, EntryType.CREDIT, payment.amount)
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contactReferenceNumber,
                    receivableAmount = BigDecimal.ZERO,
                    payableAmount = payment.amount
                )
            )
        )
}
