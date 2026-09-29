package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.util.UUID

data class SaleVoidCreateDto(
    val saleId: UUID,
    val reason: String
)
