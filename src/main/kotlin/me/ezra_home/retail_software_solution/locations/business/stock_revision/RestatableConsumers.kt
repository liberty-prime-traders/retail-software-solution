package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

// Every type a revision can replay has a ledger restatement. A type without one would move stock cost the ledger
// never sees, leaving its inventory out of step with the stock.
object RestatableConsumers {

    private val saleMovementTypes = setOf(MovementType.SALE)
    private val stockAdjustmentMovementTypes = setOf(MovementType.STOCK_ADJUSTMENT_DECREASE)
    private val restatableMovementTypes = saleMovementTypes + stockAdjustmentMovementTypes

    fun salesOf(consumerCostChanges: List<ConsumerCostChange>) =
        consumerCostChanges.filter { it.movementType in saleMovementTypes }

    fun stockAdjustmentsOf(consumerCostChanges: List<ConsumerCostChange>) =
        consumerCostChanges.filter { it.movementType in stockAdjustmentMovementTypes }

    fun problemsOf(stockEntryConsumptions: List<StockEntryConsumption>): List<BlockingProblem> =
        stockEntryConsumptions
            .filterNot { it.consumer.movementType in restatableMovementTypes }
            .map { stockEntryConsumption ->
                BlockingProblem(
                    documentReference = stockEntryConsumption.stockMovementOrigin.requiredDocumentReference(),
                    description = "Some of this delivery's stock was taken by this " +
                        "${StockConsumerLabel.of(stockEntryConsumption.consumer.movementType).lowercase()}. " +
                        "A workflow that can automatically fix this is not yet implemented. Please contact support.",
                    effect = "This line cannot be changed."
                )
            }

    fun requireAllRestatable(stockEntryConsumptions: List<StockEntryConsumption>) {
       stockEntryConsumptions
           .map { it.consumer.movementType }
           .filterNot { it in restatableMovementTypes }
           .toSet()
           .let { unrestatableMovementTypes ->
               if (unrestatableMovementTypes.isNotEmpty()) {
                   throw RtsGenericException(
                       "A stock revision cannot replay $unrestatableMovementTypes: no ledger restatement covers them."
                   )
               }
           }
    }
}
