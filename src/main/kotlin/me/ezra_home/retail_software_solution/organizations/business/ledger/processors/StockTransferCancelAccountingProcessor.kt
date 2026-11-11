package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferCancelledEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.StockTransferLedgerPostings
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockTransferCancelAccountingProcessor(
    private val stockTransferLedgerPostings: StockTransferLedgerPostings,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockTransferCancelledEvent>(ledgerPostingService) {

    override val eventType: KClass<StockTransferCancelledEvent> = StockTransferCancelledEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockTransferCancelledEvent): Boolean {
        val cancelCarriesCost = event.totalCost.toBigDecimal().signum() != 0
        val cancelAlreadyPosted = ledgerPostingGate.isPosted(
            event.sourceContext, event.dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_CANCEL
        )
        return cancelCarriesCost && !cancelAlreadyPosted
    }

    override fun prepareLedgerRequest(event: StockTransferCancelledEvent): LedgerPostingRequest {
        ledgerPostingGate.requirePosted(
            event.sourceContext, event.dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_DISPATCH
        )
        return LedgerPostingRequest(
            sourceReferenceNumber = event.dispatchReferenceNumber,
            sourceType = LedgerSourceType.STOCK_TRANSFER_CANCEL,
            postingDate = event.postingDate,
            entries = stockTransferLedgerPostings.moveOutOfTransit(event.totalCost),
            subledgerEntries = emptyList()
        )
    }
}
