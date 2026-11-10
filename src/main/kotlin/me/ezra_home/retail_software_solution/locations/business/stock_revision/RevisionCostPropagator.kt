package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCostRestater
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCostChange
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCostChange
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCostRestater
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
@TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
class RevisionCostPropagator(
    private val stockRevisionHandlerForKafka: StockRevisionHandlerForKafka,
    private val saleCostRestater: SaleCostRestater,
    private val stockAdjustmentCostRestater: StockAdjustmentCostRestater,
    private val saleCostRestatementHandlerForKafka: SaleCostRestatementHandlerForKafka,
    private val stockAdjustmentRestatementHandlerForKafka: StockAdjustmentRestatementHandlerForKafka,
) {

    fun propagate(
        stockRevisionOutcome: StockRevisionOutcome,
        purchaseId: UUID,
        replacementLine: ReplacementDeliveryLineDto
    ) {
        val stockRevision = stockRevisionOutcome.stockRevision
        val consumerCostChanges = stockRevisionOutcome.consumerCostChanges
        stockRevisionHandlerForKafka.publish(
            stockRevision, purchaseId, replacementLine.revisedLineTotal, replacementLine.replacementLineTotal
        )
        saleCostRestater.restate(
            RestatableConsumers.salesOf(consumerCostChanges)
                .map { SaleLineCostChange(it.detailReference!!, it.oldCost, it.newCost) }
        )
        saleCostRestatementHandlerForKafka.publish(stockRevision, consumerCostChanges)
        stockAdjustmentCostRestater.restate(
            RestatableConsumers.stockAdjustmentsOf(consumerCostChanges)
                .groupBy { it.documentReference }
                .map { (adjustmentReferenceNumber, costChanges) ->
                    StockAdjustmentCostChange(
                        adjustmentReferenceNumber, costChanges.sumOf { it.oldCost }, costChanges.sumOf { it.newCost }
                    )
                }
                .filter { it.oldCost.compareTo(it.newCost) != 0 }
        )
        stockAdjustmentRestatementHandlerForKafka.publish(stockRevision, consumerCostChanges)
    }
}
