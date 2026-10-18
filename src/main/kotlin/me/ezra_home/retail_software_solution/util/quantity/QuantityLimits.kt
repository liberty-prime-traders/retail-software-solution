package me.ezra_home.retail_software_solution.util.quantity

// Both limits at 2 keep price x quantity within the 4 stored decimals, so a price override total is exact
object QuantityLimits {
    const val MAX_USER_QUANTITY_DECIMAL_PLACES = 2
    const val MAX_PRICE_OVERRIDE_DECIMAL_PLACES = 2
}
