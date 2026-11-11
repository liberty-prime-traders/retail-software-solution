package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import java.util.UUID

data class ReplacementLayerPreview(
    val fifoEntries: List<StockEntryFifoDto>,
    val replacementStockEntryId: UUID
)
