package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val contactId: UUID,
    val walkInCustomer: Boolean,
    val contactName: String,
    val soldById: UUID?,
    val soldBy: String?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val status: SaleStatus,
    val paymentStatus: PaymentStatus,
    val lines: List<SaleLineResponseDto>,
    val payments: List<SalePaymentResponseDto>,
    val saleTotal: BigDecimal
)
