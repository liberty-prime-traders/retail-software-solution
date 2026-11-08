package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentAccountCodesFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.stereotype.Component

// The restated adjustments of a revision, shared by the event the save publishes and the ledger preview.
@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentRestatements(
    private val stockAdjustmentAccountCodesFetcher: StockAdjustmentAccountCodesFetcher
) {

    fun of(consumerCostChanges: List<ConsumerCostChange>): List<StockAdjustmentCostRestatementDto> {
        val adjustmentCostChanges = RestatableConsumers.stockAdjustmentsOf(consumerCostChanges)
        if (adjustmentCostChanges.isEmpty()) return emptyList()
        val accountCodesByReferenceNumber = stockAdjustmentAccountCodesFetcher
            .getByReferenceNumber(adjustmentCostChanges.map { it.documentReference })
        return adjustmentCostChanges
            .groupBy { it.documentReference }
            .map { (adjustmentReferenceNumber, costChanges) ->
                val currentRuleAccountCodes = accountCodesByReferenceNumber.getValue(adjustmentReferenceNumber)
                StockAdjustmentCostRestatementDto(
                    adjustmentReferenceNumber = adjustmentReferenceNumber,
                    currentRuleDebitAccountCode = currentRuleAccountCodes.debitAccountCode,
                    currentRuleCreditAccountCode = currentRuleAccountCodes.creditAccountCode,
                    oldCostTotal = Money.of(costChanges.sumOf { it.oldCost }),
                    newCostTotal = Money.of(costChanges.sumOf { it.newCost }),
                )
            }
            .filter { it.oldCostTotal.toBigDecimal().compareTo(it.newCostTotal.toBigDecimal()) != 0 }
    }
}
