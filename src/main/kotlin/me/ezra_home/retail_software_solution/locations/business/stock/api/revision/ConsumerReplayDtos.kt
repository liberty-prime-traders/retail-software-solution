package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.PlannedTake
import java.math.BigDecimal


// Costs cover only the part the consumer drew from the replaced layers; what it drew from other layers is untouched.
data class ConsumerReplayPlan(
    val fifoConsumptionRequest: FifoConsumptionRequest,
    val plannedTakes: List<PlannedTake>,
    val oldCost: BigDecimal,
) {
    val newCost: BigDecimal = plannedTakes.sumOf { it.movedCost }
}
