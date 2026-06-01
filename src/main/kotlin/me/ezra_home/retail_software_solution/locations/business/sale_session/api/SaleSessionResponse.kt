package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionResponseDto(
    val id: UUID,
    val referenceNumber: String?,
    val createdBy: String,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedBy: String,
    val lastAccessedAt: OffsetDateTime,
    val contactId: UUID,
    val contactLabel: String,
    val walkInCustomer: Boolean,
    val soldBy: String?,
    val saleStatus: SaleStatus,
    val paymentStatus: PaymentStatus,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val showActiveUserWarning: Boolean,
    val saleLines: List<SaleSessionLineResponse>,
    val saleAdjustments: List<SaleSessionAdjustmentResponse>,
    val salePayments: List<SaleSessionPaymentResponse>,
    val totals: SaleSessionTotals,
    val uiOptions: SaleSessionUiOptions,
)

data class SaleSessionUiOptions(
    val canMakeChangesToTheSale: Boolean,
    val canAddPaymentsToSale: Boolean,
)

data class SaleSessionLineResponse(
    val identity: SessionIdentity,
    val locationProductId: UUID,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
    val unitPriceOverride: BigDecimal,
    val netUnitPrice: BigDecimal,
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val quantityAvailable: BigDecimal,
) {
    val baseQuantity: BigDecimal = Decimals.multiplyScale4(quantity, conversionFactor)
    val lineTotal = Decimals.multiplyScale4(quantity, netUnitPrice)
}

data class SaleSessionAdjustmentResponse(
    val identity: SessionIdentity,
    val relatedSaleLineIdentity: SessionIdentity?,
    val adjustmentReason: String?,
    val direction: AdjustmentDirection,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val calculatedAmount: BigDecimal,
    val note: String?,
    val approvedBy: String?,
)

data class SaleSessionPaymentResponse(
    val identity: SessionIdentity,
    val paymentMethod: String,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val voidedReason: String?,
)
