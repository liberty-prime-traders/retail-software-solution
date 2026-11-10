package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.math.BigDecimal

// oldCost and newCost cover only what the line drew from the revised layer, so the line's cost moves by their difference.
data class SaleLineCostChange(
    val saleLineReferenceNumber: String,
    val oldCost: BigDecimal,
    val newCost: BigDecimal,
)
