package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFifoDto
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown

data class ReplayRequest(
    val stockRevisionReferenceNumber: String,
    val consumptionsOnReplacedLayer: List<StockEntryConsumption>,
    val productBeingReplayed: ProductBeingReplayed
)

data class ProductBeingReplayed(
    val productLabel: String,
    val quantityBreakdown: QuantityBreakdown,
    val fifoEntriesAvailableForConsumption: List<StockEntryFifoDto>,
)
