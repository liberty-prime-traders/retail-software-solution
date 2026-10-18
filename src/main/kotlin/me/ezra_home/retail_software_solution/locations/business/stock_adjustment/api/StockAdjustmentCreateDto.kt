package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.math.BigDecimal
import java.util.UUID

data class StockAdjustmentCreateDto(
    val locationProductId: UUID,
    val category: StockAdjustmentCategory,
    val direction: StockAdjustmentDirection? = null,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val stockMovementReasonId: UUID? = null,
    val note: String? = null,
    val unitCost: BigDecimal? = null
) {
    init {
        Decimals.requireMaxDecimalPlaces(userProvidedQuantity, QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES, "Quantity")
        if (unitCost != null) {
            Decimals.requireMaxDecimalPlaces(unitCost, QuantityLimits.MAX_PRICE_OVERRIDE_DECIMAL_PLACES, "Unit cost")
        }
    }
}
