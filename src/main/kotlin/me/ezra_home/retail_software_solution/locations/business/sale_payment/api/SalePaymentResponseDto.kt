package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val saleId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val paymentMethodName: String,
    val voidedReason: String? = null,
    val updatedSalePaymentStatus: PaymentStatus? = null,
    val updatedSaleVersion: Long? = null,
)
