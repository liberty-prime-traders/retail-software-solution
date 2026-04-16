package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZoneId
import kotlin.reflect.KClass

@Service
class PurchaseDeliveryAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<PurchaseDeliveredEvent>(ledgerPostingService) {

    override val eventType: KClass<PurchaseDeliveredEvent> = PurchaseDeliveredEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: PurchaseDeliveredEvent): Boolean {
        return ledgerEntryGroupRepository.existsBySourceReferenceNumber(event.deliveryReferenceNumber).not()
    }

    override fun prepareLedgerRequest(event: PurchaseDeliveredEvent): LedgerPostingRequest {
        val postingDate = event.deliveredAt
            .atZone(ZoneId.of(SessionContextProvider.getOrgTimezone()))
            .toLocalDate()

        return LedgerPostingRequest(
            sourceReferenceNumber = event.deliveryReferenceNumber,
            sourceType = LedgerSourceType.PURCHASE_DELIVERY,
            postingDate = postingDate,
            entries = getLedgerEntries(event),
            subledgerEntries = getSubLedgerEntries(event)
        )
    }

    private fun getLedgerEntries(event: PurchaseDeliveredEvent): List<LedgerEntryRequest> {
        val total = event.deliveryTotal
        return listOf(
            LedgerEntryRequest(SystemAccount.INVENTORY.code, EntryType.DEBIT, total),
            LedgerEntryRequest(SystemAccount.TRADE_PAYABLES.code, EntryType.CREDIT, total)
        )
    }
    private fun getSubLedgerEntries(event: PurchaseDeliveredEvent): List<SubledgerEntryRequest> {
        val supplier = contactService.getAllContactDtos().firstOrNull { it.id == event.supplierId }
                ?: throw RtsGenericException("Supplier ${event.supplierId} not found")

        return listOf(
            SubledgerEntryRequest(
                contactReferenceNumber = supplier.referenceNumber,
                payableAmount = event.deliveryTotal,
                receivableAmount = BigDecimal.ZERO
            )
        )
    }
}
