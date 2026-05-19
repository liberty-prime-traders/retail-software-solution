package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionResponseDto(
    val sessionId: UUID,
    val saleId: UUID?,
    val saleVersion: Long?,
    val locationId: UUID,
    val createdById: UUID,
    val createdByLabel: String?,
    val createdAt: OffsetDateTime,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedById: UUID,
    val lastAccessedByLabel: String?,
    val lastAccessedAt: OffsetDateTime,
    val contactId: UUID,
    val contactLabel: String,
    val walkInCustomer: Boolean,
    val soldById: UUID?,
    val soldByLabel: String?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val lines: List<SaleSessionLineDto>,
    val adjustments: List<SaleSessionAdjustmentDto>,
    val payments: List<SaleSessionPaymentDto>,
    val totals: SaleSessionTotals,
)

data class SaleSessionLineDto(
    val identity: SessionIdentity,
    val locationProductId: UUID,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
)

data class SaleSessionAdjustmentDto(
    val identity: SessionIdentity,
    val lineIdentity: SessionIdentity?,
    val adjustmentReasonId: UUID,
    val adjustmentReasonLabel: String?,
    val direction: AdjustmentDirection,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val calculatedAmount: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
    val approvedByLabel: String?,
)

data class SaleSessionPaymentDto(
    val identity: SessionIdentity,
    val paymentMethod: String?,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val voidedReason: String?,
)
