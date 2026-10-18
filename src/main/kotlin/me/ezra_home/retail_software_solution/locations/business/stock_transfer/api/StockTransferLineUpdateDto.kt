package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

data class StockTransferLineUpdateDto(
    val lineRef: String,
    val quantityDispatched: BigDecimal?,
    val unitId: UUID?
) {
    init {
        quantityDispatched?.let { Decimals.requireMaxDecimalPlaces(it, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity") }
    }
}
