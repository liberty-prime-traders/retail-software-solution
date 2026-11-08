package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import java.math.BigDecimal

data class ConsumerCostChange(
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String?,
    val oldCost: BigDecimal,
    val newCost: BigDecimal,
) {

    companion object {

        fun ofPlans(consumerReplayPlans: List<ConsumerReplayPlan>): List<ConsumerCostChange> =
            consumerReplayPlans.map { consumerReplayPlan ->
                val fifoConsumptionRequest = consumerReplayPlan.fifoConsumptionRequest
                ConsumerCostChange(
                    movementType = fifoConsumptionRequest.movementType,
                    documentReference = fifoConsumptionRequest.documentReference,
                    detailReference = fifoConsumptionRequest.detailReference,
                    oldCost = consumerReplayPlan.oldCost,
                    newCost = consumerReplayPlan.newCost,
                )
            }

        // The restore movements carry the old cost and the replayed draws the new one, both tagged with the revision.
        fun ofRevisionMovements(revisionMovements: List<StockMovementDto>): List<ConsumerCostChange> {
            val oldCostByConsumer = revisionMovements
                .filter { it.movementType == MovementType.REVERSED_BY_REVISION }
                .groupBy { it.documentReference to it.detailReference }
                .mapValues { (_, movements) -> movements.sumOf { it.movedCost } }
            return revisionMovements
                .filter { it.movementType.isConsumption() }
                .groupBy { it.documentReference to it.detailReference }
                .map { (consumerKey, movements) ->
                    ConsumerCostChange(
                        movementType = movements.first().movementType,
                        documentReference = consumerKey.first!!,
                        detailReference = consumerKey.second,
                        oldCost = oldCostByConsumer.getValue(consumerKey),
                        newCost = movements.sumOf { it.movedCost },
                    )
                }
        }
    }
}
