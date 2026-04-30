package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SupplierPaymentCreateDto(
    val purchaseId: UUID,
    val deliveryId: UUID?,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val paymentDate: OffsetDateTime,
    val notes: String?
)
