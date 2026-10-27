package me.ezra_home.retail_software_solution.util.quantity

import java.math.BigDecimal

object QuantityLimits {
    const val MAX_USER_QUANTITY_DECIMAL_PLACES = 2

    // User-provided columns are numeric(19,4), 15 integer digits; 12 leave room for balances to sum. Independent of the money ceiling.
    val MAX_USER_QUANTITY: BigDecimal = BigDecimal("999999999999.99")
}
