package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import java.util.UUID

data class SalePaymentVoidCreateDto(
    val salePaymentId: UUID,
    val reason: String
)
