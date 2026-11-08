package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.AccountingEventProcessor
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.PostedStockAdjustmentAccounts
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.PostedStockAdjustmentAccountsReader
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
        return event.restatements
            .flatMap { restatement -> legsOf(restatement, postedAccounts[restatement.adjustmentReferenceNumber]) }
            .groupBy { leg -> leg.accountCode }
            .toSortedMap()
            .flatMap { (accountCode, legs) -> entriesOf(accountCode, legs.sumOf { leg -> leg.debitMinusCredit }) }
    }

    // The accounts are the ones the original posting used, so a later change of the category rules never splits an
    // adjustment across two sets of accounts. A cost that rises debits the adjustment's debit account again; a cost
    // that falls credits it, and the credit account moves the opposite way.
    private fun legsOf(
        restatement: StockAdjustmentCostRestatementDto,
        postedAccounts: PostedStockAdjustmentAccounts?
    ): List<AccountLeg> {
        val costChange = restatement.newCostTotal.toBigDecimal().subtract(restatement.oldCostTotal.toBigDecimal())
        return listOf(
            AccountLeg(postedAccounts?.debitAccountCode ?: restatement.currentRuleDebitAccountCode, costChange),
            AccountLeg(
                postedAccounts?.creditAccountCode ?: restatement.currentRuleCreditAccountCode,
                costChange.negate()
            )
        )
    }

    private fun entriesOf(accountCode: String, debitMinusCredit: BigDecimal): List<LedgerEntryRequest> =
        if (debitMinusCredit.signum() > 0) {
            LedgerEntryRequest.debitUnlessZero(accountCode, debitMinusCredit)
        } else {
            LedgerEntryRequest.creditUnlessZero(accountCode, debitMinusCredit.negate())
        }

    private data class AccountLeg(val accountCode: String, val debitMinusCredit: BigDecimal)
}
