package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferReceiptCompletedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.StockTransferLedgerPostings
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

// Posts at the destination location, keyed by the dispatch's reference; the dispatch it clears was posted at the source.
@Service
class StockTransferReceiptAccountingProcessor(
    private val stockTransferLedgerPostings: StockTransferLedgerPostings,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockTransferReceiptCompletedEvent>(ledgerPostingService) {

    override val eventType: KClass<StockTransferReceiptCompletedEvent> = StockTransferReceiptCompletedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockTransferReceiptCompletedEvent): Boolean {
        val receiptCarriesCost = event.totalCost.toBigDecimal().signum() != 0
        val receiptAlreadyPosted = ledgerPostingGate.isPosted(
            event.sourceContext, event.dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_RECEIPT
        )
        return receiptCarriesCost && !receiptAlreadyPosted
    }

    override fun prepareLedgerRequest(event: StockTransferReceiptCompletedEvent): LedgerPostingRequest {
        stockTransferLedgerPostings.requireDispatchPosted(event.orderReferenceNumber, event.dispatchReferenceNumber)
        return LedgerPostingRequest(
            sourceReferenceNumber = event.dispatchReferenceNumber,
            sourceType = LedgerSourceType.STOCK_TRANSFER_RECEIPT,
            postingDate = event.postingDate,
            entries = stockTransferLedgerPostings.moveOutOfTransit(event.totalCost),
            subledgerEntries = emptyList()
        )
    }
}
