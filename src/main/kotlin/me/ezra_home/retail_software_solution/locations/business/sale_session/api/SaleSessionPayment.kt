package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionPayment(
    val id: SessionIdentity,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
)
