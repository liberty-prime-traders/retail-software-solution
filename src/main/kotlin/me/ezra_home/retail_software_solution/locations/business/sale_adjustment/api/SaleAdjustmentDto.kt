package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.util.UUID

data class SaleAdjustmentDto(
    val id: UUID,
    val saleLineId: UUID?,
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val note: String?,
    val approvedById: UUID?,
)

data class SaleAdjustmentSummaryDto(
    val saleLineId: UUID?,
    val direction: AdjustmentDirection,
    val calculatedAmount: BigDecimal,
)
