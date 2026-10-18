package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

data class OpeningStockLineDto(
    val locationProductId: UUID,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val unitCost: BigDecimal
) {
    init {
        Decimals.requireMaxDecimalPlaces(userProvidedQuantity, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
    }
}
