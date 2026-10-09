package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSummary(
    val id: UUID,
    val referenceNumber: String,
    val contactName: String,
    val soldBy: String?,
    val dateSold: OffsetDateTime?,
    val status: SaleStatus,
    val paymentStatus: PaymentStatus,
    val arrearsTotal: BigDecimal,
    val totalPaid: BigDecimal
)
