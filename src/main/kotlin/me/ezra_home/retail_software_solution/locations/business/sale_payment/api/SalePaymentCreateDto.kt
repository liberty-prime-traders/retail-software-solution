package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentCreateDto(
    val saleId: UUID? = null,
    val paymentMethodId: UUID,
    val amount: PositiveMoney,
    val reference: String? = null,
    val paymentDate: OffsetDateTime? = null
)
