package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class StockAdjustmentCreateDto(
    val locationProductId: UUID,
    val category: StockAdjustmentCategory,
    val direction: StockAdjustmentDirection? = null,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: PositiveUserProvidedQuantity,
    val unitId: UUID,
    val stockMovementReasonId: UUID? = null,
    val note: String? = null,
    val unitCost: PositiveMoney? = null
) {

    fun requiredUnitCost(): PositiveMoney = unitCost ?: throw RtsGenericException("Unit cost is required for a stock increase")
}
