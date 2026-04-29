package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SupplierPaymentResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val purchaseId: UUID,
    val purchaseReferenceNumber: String,
    val deliveryReferenceNumber: String? = null,
    val paymentMethod: String,
    val supplier: String,
    val amount: BigDecimal,
    val paymentDate: OffsetDateTime,
    val notes: String?,
    val createdBy: String,
    val createdOn: String,
    val voidedReason: String? = null,
    val updatedPurchasePaymentStatus: PaymentStatus? = null
)
