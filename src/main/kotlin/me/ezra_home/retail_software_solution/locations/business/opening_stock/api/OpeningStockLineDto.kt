package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class OpeningStockLineDto(
    val locationProductId: UUID,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: PositiveUserProvidedQuantity,
    val unitId: UUID,
    val unitCost: PositiveMoney
)
