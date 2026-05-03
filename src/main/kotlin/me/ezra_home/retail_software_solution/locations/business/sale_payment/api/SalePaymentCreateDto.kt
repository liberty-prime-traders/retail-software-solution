package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentCreateDto(
    val saleId: UUID? = null,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String? = null,
    val paymentDate: OffsetDateTime? = null
)
