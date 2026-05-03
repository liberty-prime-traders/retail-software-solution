package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.math.BigDecimal
import java.util.UUID

data class SaleLineUpdateDto(
    val id: UUID,
    val locationProductId: UUID,
    val quantity: BigDecimal,
    val unitId: UUID,
    val unitPrice: BigDecimal
)
