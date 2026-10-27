package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSaveRequest(
    val saleId: UUID?,
    val expectedVersion: Long?,
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val saleLines: List<SaleLineSaveRequest>,
    val saleAdjustments: List<SaleAdjustmentSaveRequest>,
    val salePayments: List<SalePaymentSaveRequest>,
)

data class SaleLineSaveRequest(
    val clientKey: UUID,
    val existingId: UUID?,
    val locationProductId: UUID,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val lineTotal: BigDecimal,
    val defaultSalePrice: BigDecimal,
)

data class SaleAdjustmentSaveRequest(
    val clientKey: UUID,
    val existingId: UUID?,
    val relatedSaleLineClientKey: UUID?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
)

data class SalePaymentSaveRequest(
    val clientKey: UUID,
    val existingId: UUID?,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
)

