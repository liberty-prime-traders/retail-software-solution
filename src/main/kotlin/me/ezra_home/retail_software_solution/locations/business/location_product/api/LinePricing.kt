package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object LinePricing {

    fun overrideTotal(userProvidedUnitPrice: BigDecimal, userProvidedQuantity: BigDecimal): BigDecimal =
        Decimals.MoneyScale.multiply(userProvidedUnitPrice, userProvidedQuantity)

    // Positive when the override total is lower than the line total (a discount), negative for a surcharge.
    fun overrideDifference(lineTotal: BigDecimal, userProvidedUnitPrice: BigDecimal, userProvidedQuantity: BigDecimal): BigDecimal =
        lineTotal.subtract(overrideTotal(userProvidedUnitPrice, userProvidedQuantity))
}
