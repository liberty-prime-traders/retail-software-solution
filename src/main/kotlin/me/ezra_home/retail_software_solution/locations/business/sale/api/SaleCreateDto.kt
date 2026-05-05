package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import java.time.OffsetDateTime
import java.util.UUID

data class SaleCreateDto(
    val contactId: UUID? = null,
    val walkInCustomer: Boolean = false,
    val soldBy: UUID? = null,
    val dateSold: OffsetDateTime? = null,
    val notes: String? = null,
    val linesToAdd: List<SaleLineCreateDto>,
    val payments: List<SalePaymentCreateDto>
)
