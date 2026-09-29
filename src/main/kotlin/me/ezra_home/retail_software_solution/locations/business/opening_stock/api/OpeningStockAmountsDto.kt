package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import java.math.BigDecimal

data class OpeningStockAmountsDto(
    val quantity: BigDecimal,
    val unitCost: BigDecimal
)
