package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveryRevisedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionLedgerEntries
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.SubledgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class PurchaseDeliveryRevisionAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<PurchaseDeliveryRevisedEvent>(ledgerPostingService) {

    override val eventType: KClass<PurchaseDeliveryRevisedEvent> = PurchaseDeliveryRevisedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: PurchaseDeliveryRevisedEvent): Boolean =
        ledgerPostingGate.isPosted(
            event.sourceContext, event.stockRevisionReferenceNumber, LedgerSourceType.PURCHASE_DELIVERY_REVISION
        ).not()

    override fun prepareLedgerRequest(event: PurchaseDeliveryRevisedEvent): LedgerPostingRequest {
        val supplier = contactService.getContactById(event.supplierId)
        val revisedLineTotal = event.revisedLineTotal.toBigDecimal()
        val replacementLineTotal = event.replacementLineTotal.toBigDecimal()
        val changeInLineTotal = replacementLineTotal.subtract(revisedLineTotal)
        val increasesAmountOwed = changeInLineTotal.signum() > 0
        val amount = changeInLineTotal.abs()

        return LedgerPostingRequest(
            sourceReferenceNumber = event.stockRevisionReferenceNumber,
            sourceType = LedgerSourceType.PURCHASE_DELIVERY_REVISION,
            postingDate = event.postingDate,
            entries = RevisionLedgerEntries.purchaseDeliveryRevision(
                revisedLineTotal, replacementLineTotal
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = supplier.referenceNumber,
                    payableAmount = if (increasesAmountOwed) Money.of(amount) else Money.ZERO,
                    receivableAmount = if (increasesAmountOwed) Money.ZERO else Money.of(amount)
                )
            )
        )
    }
}
