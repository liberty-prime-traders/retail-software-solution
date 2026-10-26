package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class StockTransferLineInsertDto(
    val locationProductId: UUID,
    val quantityDispatched: PositiveUserProvidedQuantity
)
