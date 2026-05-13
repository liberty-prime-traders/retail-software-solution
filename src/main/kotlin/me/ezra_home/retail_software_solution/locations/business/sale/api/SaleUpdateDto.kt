package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentCreateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

data class SaleUpdateDto(
    val id: UUID,
    val contactId: Optional<UUID>? = null,
    val soldById: Optional<UUID>? = null,
    val dateSold: Optional<OffsetDateTime>? = null,
    val notes: Optional<String>? = null,
    val linesToAdd: List<SaleLineCreateDto> = emptyList(),
    val linesToUpdate: List<SaleLineUpdateDto> = emptyList(),
    val linesToRemove: List<UUID> = emptyList(),
    val payments: List<SalePaymentCreateDto> = emptyList(),
    val discountsToAdd: List<SaleDiscountCreateDto> = emptyList(),
    val discountsToRemove: List<UUID> = emptyList()
) {

    fun applyTo(sale: SaleEntity) {
        contactId?.let {
            sale.contactId = it.orElseThrow { RtsGenericException("contactId cannot be null") }
        }
        soldById?.let { sale.soldById = it.orElse(null) }
        dateSold?.let { sale.dateSold = it.orElse(null) }
        notes?.let { sale.notes = it.orElse(null) }
    }
}
