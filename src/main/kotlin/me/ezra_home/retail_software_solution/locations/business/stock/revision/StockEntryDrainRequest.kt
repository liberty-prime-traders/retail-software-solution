package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown

data class StockEntryDrainRequest(
    val stockEntry: StockEntryEntity,
    val stockRevisionReferenceNumber: String,
    val balanceBeforeDrain: ExactQuantity,
    val quantityBreakdown: QuantityBreakdown,
)
