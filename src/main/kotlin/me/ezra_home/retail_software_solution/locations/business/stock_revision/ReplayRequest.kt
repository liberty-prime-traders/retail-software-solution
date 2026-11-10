package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown

data class ReplayRequest(
    val stockRevisionReferenceNumber: String,
    val consumptionsOnReplacedLayer: List<StockEntryConsumption>,
    val productBeingReplayed: ProductBeingReplayed
)

data class ProductBeingReplayed(
    val productName: String,
    val quantityBreakdown: QuantityBreakdown,
    val fifoEntriesAvailableForConsumption: List<StockEntryFifoDto>,
)

data class ReplayResult(
    val consumerReplayPlans: List<ConsumerReplayPlan>,
    val problems: List<BlockingProblem>,
    val totalTakenByConsumers: ExactQuantity = ExactQuantity.ZERO,
    val movementTypesOfUncoveredConsumers: Set<MovementType> = emptySet(),
)
