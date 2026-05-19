package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.math.BigDecimal
import java.util.UUID

data class SaleLineDto(
    val id: UUID,
    val locationProductId: UUID,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
)
