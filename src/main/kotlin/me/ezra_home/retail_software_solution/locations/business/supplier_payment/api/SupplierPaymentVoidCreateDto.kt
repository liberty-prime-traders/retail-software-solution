package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import java.time.LocalDate
import java.util.UUID

data class SupplierPaymentVoidCreateDto(
    val supplierPaymentId: UUID,
    val reason: String,
    val voidedOn: LocalDate
)
