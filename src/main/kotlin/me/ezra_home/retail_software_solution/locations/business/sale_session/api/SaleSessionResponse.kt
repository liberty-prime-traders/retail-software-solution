package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import com.fasterxml.jackson.annotation.JsonProperty
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleVoidInfoDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
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
    val saleVoidInfo: SaleVoidInfoDto?,
    val saleLines: List<SaleSessionLineResponse>,
    val saleAdjustments: List<SaleSessionAdjustmentResponse>,
    val salePayments: List<SaleSessionPaymentResponse>,
    val totals: SaleSessionTotals,
    val uiOptions: SaleSessionUiOptions,
)

data class SaleSessionUiOptions(
    val canMakeChangesToTheSale: Boolean,
    val canAddPaymentsToSale: Boolean,
    val showUnreservedChangesWarning: Boolean,
    val showActiveUserWarning: Boolean
)

data class SaleSessionLineResponse(
    val identity: SessionIdentity,
    val locationProductId: UUID,
    val productLabel: String,
    val productPrintLabel: String,
    @get:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
    val unitPriceOverride: BigDecimal,
    val netUnitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    @get:JsonProperty("baseQuantity")
    val quantityInProductBaseUnit: BigDecimal,
    @get:JsonProperty("quantityOnHand")
    val onHandInProductBaseUnit: BigDecimal,
    @get:JsonProperty("quantityReserved")
    val reservedInProductBaseUnit: BigDecimal,
    @get:JsonProperty("quantityAvailable")
    val availableInProductBaseUnit: BigDecimal,
    val storageQuantityOnHand: BigDecimal,
    val storageQuantityReserved: BigDecimal,
    val storageQuantityAvailable: BigDecimal,
    val quantityOnHandBreakdown: String,
    val quantityAvailableBreakdown: String,
)

data class SaleSessionAdjustmentResponse(
    val identity: SessionIdentity,
    val relatedSaleLineIdentity: SessionIdentity?,
    val adjustmentReason: String?,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
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
