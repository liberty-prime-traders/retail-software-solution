package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.RevisionLedgerEntries
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.PostedStockAdjustmentAccountsReader
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockAdjustmentRestatementAccountingProcessor(
    private val ledgerPostingGate: LedgerPostingGate,
    private val postedStockAdjustmentAccountsReader: PostedStockAdjustmentAccountsReader,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockAdjustmentCostRestatedEvent>(ledgerPostingService) {

    override val eventType: KClass<StockAdjustmentCostRestatedEvent> = StockAdjustmentCostRestatedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockAdjustmentCostRestatedEvent): Boolean =
        ledgerPostingGate.isPosted(
            event.sourceContext, event.stockRevisionReferenceNumber, LedgerSourceType.STOCK_ADJUSTMENT_RESTATEMENT
        ).not()

    override fun prepareLedgerRequest(event: StockAdjustmentCostRestatedEvent): LedgerPostingRequest =
        LedgerPostingRequest(
            sourceReferenceNumber = event.stockRevisionReferenceNumber,
            sourceType = LedgerSourceType.STOCK_ADJUSTMENT_RESTATEMENT,
            postingDate = event.postingDate,
            entries = entriesOf(event),
            subledgerEntries = emptyList()
        )

    private fun entriesOf(event: StockAdjustmentCostRestatedEvent): List<LedgerEntryRequest> {
        val adjustmentReferenceNumbers = event.restatements.map { it.adjustmentReferenceNumber }.toSet()
        val postedAccounts =
            postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(adjustmentReferenceNumbers)
        return RevisionLedgerEntries.stockAdjustmentRestatement(
            event.restatements.map { restatement ->
                RevisionLedgerEntries.adjustmentCostChangeOf(
                    restatement, postedAccounts[restatement.adjustmentReferenceNumber]
                )
            }
        )
    }
}
