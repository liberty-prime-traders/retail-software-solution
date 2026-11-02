package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity

/** One row of a bulk opening stock upload. Products and units are named by their human-facing
 * reference number and unit code; [defaultSalePrice] is written to the location product.
 * [unitCost] is priced per [costUnitCode], which defaults to [unitCode]. A zero [userProvidedQuantity] declares no
 * opening stock and only sets the price; any positive quantity needs a [unitCost]. */
data class OpeningStockBulkLineDto(
    val locationProductReferenceNumber: String,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: UserProvidedQuantity,
    val unitCode: String,
    val unitCost: PositiveMoney? = null,
    val costUnitCode: String? = null,
    val defaultSalePrice: Money
)
