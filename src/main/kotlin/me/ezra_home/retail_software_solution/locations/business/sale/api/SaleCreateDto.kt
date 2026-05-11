package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.OffsetDateTime
import java.util.UUID

data class SaleCreateDto(
    val contactId: UUID? = null,
    val walkInCustomer: Boolean = false,
    val soldBy: UUID? = null,
    val dateSold: OffsetDateTime? = null,
    val notes: String? = null,
    val linesToAdd: List<SaleLineCreateDto>,
    val payments: List<SalePaymentCreateDto>,
    val discounts: List<SaleDiscountCreateDto> = emptyList()
) {

    fun resolveContactId(): UUID {
        return if (walkInCustomer) {
            SystemContact.WALK_IN.id
        } else {
            contactId ?: throw RtsGenericException("Customer is required for non-walk-in sales")
        }
    }
}
