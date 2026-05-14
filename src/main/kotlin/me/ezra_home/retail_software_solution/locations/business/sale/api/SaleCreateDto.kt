package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import java.time.OffsetDateTime
import java.util.UUID

data class SaleCreateDto(
    val contactId: UUID? = null,
    val soldById: UUID?,
    val dateSold: OffsetDateTime?,
    val notes: String? = null,
    val linesToAdd: List<SaleLineCreateDto>,
    val payments: List<SalePaymentCreateDto>,
    val discounts: List<SaleDiscountCreateDto> = emptyList()
) {

    fun walkInCustomer(): Boolean = resolveContactId() == SystemContact.WALK_IN.id

    fun resolveContactId(): UUID {
        return contactId?.takeIf { it != SystemContact.WALK_IN.id } ?: SystemContact.WALK_IN.id
    }
}
