package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.PlannedTake
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import java.math.BigDecimal
import java.util.UUID


data class ConsumerReplayDto(
    val locationProductId: UUID,
    val movementType: MovementType,
    val documentReference: String?,
    val detailReference: String?,
    val oldCost: BigDecimal,
    val newCost: BigDecimal,
)


// Costs cover only the part the consumer drew from the replaced layers; what it drew from other layers is untouched.
data class ConsumerReplayPlan(
    val fifoConsumptionRequest: FifoConsumptionRequest,
    val plannedTakes: List<PlannedTake>,
    val oldCost: BigDecimal,
) {
    val newCost: BigDecimal = plannedTakes.sumOf { it.movedCost }
}
