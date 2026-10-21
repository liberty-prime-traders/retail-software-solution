package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockAdjustmentAccountingProcessor(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockAdjustmentRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<StockAdjustmentRecordedEvent> = StockAdjustmentRecordedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockAdjustmentRecordedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.ledgerSourceReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: StockAdjustmentRecordedEvent): LedgerPostingRequest {
        return LedgerPostingRequest(
            sourceReferenceNumber = event.ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.STOCK_ADJUSTMENT,
            postingDate = event.postingDate,
            entries = listOf(
                LedgerEntryRequest.debit(event.debitAccountCode, event.totalCost),
                LedgerEntryRequest.credit(event.creditAccountCode, event.totalCost)
            ),
            subledgerEntries = emptyList()
        )
    }
}
