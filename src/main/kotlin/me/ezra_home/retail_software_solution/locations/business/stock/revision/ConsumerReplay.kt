package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.api.PlannedTake
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import java.math.BigDecimal
import java.util.UUID

// What one consumer will draw again, worked out but not written.
// Costs cover only the part the consumer drew from the replaced layers; what it drew from other layers is untouched.
data class ConsumerReplayPlan(
    val fifoConsumptionRequest: FifoConsumptionRequest,
    val plannedTakes: List<PlannedTake>,
    val oldCost: BigDecimal,
) {
    val newCost: BigDecimal = plannedTakes.sumOf { it.movedCost }
}

data class ReplayResult(
    val consumerReplayPlans: List<ConsumerReplayPlan>,
    val errors: List<String>,
)

// A plan after stock has applied it.
data class ConsumerReplay(
    val locationProductId: UUID,
    val movementType: MovementType,
    val documentReference: String?,
    val detailReference: String?,
    val oldCost: BigDecimal,
    val newCost: BigDecimal,
    val movements: List<StockMovementEntity>,
)
