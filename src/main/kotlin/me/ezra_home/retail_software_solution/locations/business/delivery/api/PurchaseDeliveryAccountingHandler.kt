package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZoneId

@Service
class PurchaseDeliveryAccountingHandler(
    private val ledgerPostingService: LedgerPostingService,
    private val contactService: ContactService
) {

    private val log = LoggerFactory.getLogger(PurchaseDeliveryAccountingHandler::class.java)

    fun handle(event: PurchaseDeliveredEvent) {
        try {
            val supplier = contactService.getAllContactDtos().firstOrNull { it.id == event.supplierId }
                ?: throw RtsGenericException("Supplier ${event.supplierId} not found")

            val postingDate = event.deliveredAt
                ?.atZone(ZoneId.of(SessionContextProvider.getOrgTimezone()))
                ?.toLocalDate()
                ?: DateTimes.Local.Now.organization()
            val deliveryTotal = event.deliveryTotal

            ledgerPostingService.post(
                LedgerPostingRequest(
                    sourceReferenceNumber = event.deliveryReferenceNumber,
                    sourceType = LedgerSourceType.PURCHASE_DELIVERY,
                    postingDate = postingDate,
                    entries = listOf(
                        LedgerEntryRequest(SystemAccount.INVENTORY.code, EntryType.DEBIT, deliveryTotal),
                        LedgerEntryRequest(SystemAccount.TRADE_PAYABLES.code, EntryType.CREDIT, deliveryTotal)
                    ),
                    subledgerEntry = SubledgerEntryRequest(
                        contactReferenceNumber = supplier.referenceNumber,
                        payableAmount = deliveryTotal,
                        receivableAmount = BigDecimal.ZERO
                    )
                )
            )
        } catch (e: Exception) {
            log.error("Failed to post accounting entries for delivery ${event.deliveryId}", e)
            throw e
        }
    }
}
