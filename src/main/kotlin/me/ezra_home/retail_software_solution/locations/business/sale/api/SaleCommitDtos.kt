package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleCommitInput(
    val saleId: UUID?,
    val expectedVersion: Long?,
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
    val lines: List<SaleCommitLine>,
    val adjustments: List<SaleCommitAdjustment>,
    val payments: List<SaleCommitPayment>,
)

data class SaleCommitLine(
    val clientKey: UUID,
    val existingId: UUID?,
    val locationProductId: UUID,
    val quantity: BigDecimal,
    val unitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
)

data class SaleCommitAdjustment(
    val clientKey: UUID,
    val existingId: UUID?,
    val lineClientKey: UUID?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
)

data class SaleCommitPayment(
    val clientKey: UUID,
    val existingId: UUID?,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
)

data class SaleCommitOutcome(
    val saleId: UUID,
    val saleReferenceNumber: String,
    val newVersion: Long,
    val lineIdsByClientKey: Map<UUID, UUID>,
    val adjustmentIdsByClientKey: Map<UUID, UUID>,
    val paymentIdsByClientKey: Map<UUID, UUID>,
)
