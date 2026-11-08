package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleCostRestatedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SaleCogsRestatementAccountingProcessor(
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SaleCostRestatedEvent>(ledgerPostingService) {

    override val eventType: KClass<SaleCostRestatedEvent> = SaleCostRestatedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SaleCostRestatedEvent): Boolean =
        ledgerPostingGate.isPosted(
            event.sourceContext, event.stockRevisionReferenceNumber, LedgerSourceType.SALE_COGS_RESTATEMENT
        ).not()

    override fun prepareLedgerRequest(event: SaleCostRestatedEvent): LedgerPostingRequest {
        val costChange = event.newCostTotal.toBigDecimal().subtract(event.oldCostTotal.toBigDecimal())
        val amount = costChange.abs()
        val entries = buildList {
            if (costChange.signum() > 0) {
                addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, amount))
                addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.INVENTORY.code, amount))
            } else {
                addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, amount))
                addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, amount))
            }
        }
        return LedgerPostingRequest(
            sourceReferenceNumber = event.stockRevisionReferenceNumber,
            sourceType = LedgerSourceType.SALE_COGS_RESTATEMENT,
            postingDate = event.postingDate,
            entries = entries,
            subledgerEntries = emptyList()
        )
    }
}
