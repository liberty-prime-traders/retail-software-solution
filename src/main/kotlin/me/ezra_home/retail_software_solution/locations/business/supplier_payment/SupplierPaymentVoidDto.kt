package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import java.time.OffsetDateTime
import java.util.UUID

data class SupplierPaymentVoidDto(
    val id: UUID,
    val referenceNumber: String,
    val supplierPaymentId: UUID,
    val reason: String,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
