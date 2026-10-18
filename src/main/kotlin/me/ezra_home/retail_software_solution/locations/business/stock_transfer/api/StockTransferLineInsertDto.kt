package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

data class StockTransferLineInsertDto(
    val locationProductId: UUID,
    val quantityDispatched: BigDecimal
) {
    init {
        Decimals.requireMaxDecimalPlaces(quantityDispatched, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
    }
}
