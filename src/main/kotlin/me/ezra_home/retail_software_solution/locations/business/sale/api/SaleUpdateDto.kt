package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

data class SaleUpdateDto(
    val id: UUID,
    val contactId: Optional<UUID>? = null,
    val walkInCustomer: Boolean = false,
    val soldBy: UUID? = null,
    val dateSold: OffsetDateTime? = null,
    val notes: String? = null,
    val linesToAdd: List<SaleLineCreateDto> = emptyList(),
    val linesToUpdate: List<SaleLineUpdateDto> = emptyList(),
    val payments: List<SalePaymentCreateDto> = emptyList()
)
