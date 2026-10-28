package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class PurchaseDeliveryAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<PurchaseDeliveredEvent>(ledgerPostingService) {

    override val eventType: KClass<PurchaseDeliveredEvent> = PurchaseDeliveredEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: PurchaseDeliveredEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.deliveryReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: PurchaseDeliveredEvent): LedgerPostingRequest {
        val postingDate = DateTimes.Local.atOrganizationZone(event.deliveredAt)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.deliveryReferenceNumber,
            sourceType = LedgerSourceType.PURCHASE_DELIVERY,
            postingDate = postingDate,
            entries = getLedgerEntries(event),
            subledgerEntries = getSubLedgerEntries(event)
        )
    }

    private fun getLedgerEntries(event: PurchaseDeliveredEvent): List<LedgerEntryRequest> {
        val total = event.deliveryTotal.toBigDecimal()
        return LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, total) +
            LedgerEntryRequest.creditUnlessZero(SystemAccount.TRADE_PAYABLES.code, total)
    }
    private fun getSubLedgerEntries(event: PurchaseDeliveredEvent): List<SubledgerEntryRequest> {
        val supplier = contactService.getContactById(event.supplierId)

        return listOf(
            SubledgerEntryRequest(
                contactReferenceNumber = supplier.referenceNumber,
                payableAmount = event.deliveryTotal,
                receivableAmount = Money.ZERO
            )
        )
    }
}
