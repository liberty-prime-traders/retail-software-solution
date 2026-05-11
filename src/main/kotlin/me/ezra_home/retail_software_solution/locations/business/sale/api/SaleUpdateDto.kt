package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
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
    val linesToRemove: List<UUID> = emptyList(),
    val payments: List<SalePaymentCreateDto> = emptyList(),
    val discountsToAdd: List<SaleDiscountCreateDto> = emptyList(),
    val discountsToRemove: List<UUID> = emptyList()
) {

    fun applyContactId(sale: SaleEntity) {
        when {
            walkInCustomer -> sale.contactId = SystemContact.WALK_IN.id
            contactId != null -> sale.contactId = contactId.orElseThrow {
                RtsGenericException("contactId cannot be null")
            }
        }
    }
}
