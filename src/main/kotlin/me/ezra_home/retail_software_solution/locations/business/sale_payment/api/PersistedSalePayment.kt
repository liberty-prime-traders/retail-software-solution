package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import java.time.OffsetDateTime
import java.util.UUID

data class PersistedSalePayment(
    val id: UUID,
    val paymentDate: OffsetDateTime,
)
