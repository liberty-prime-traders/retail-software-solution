package me.ezra_home.retail_software_solution.util.quantity

import java.math.BigDecimal

object QuantityLimits {
    const val MAX_USER_QUANTITY_DECIMAL_PLACES = 2

    val MAX_USER_QUANTITY: BigDecimal = BigDecimal("999999999999.99")
}
