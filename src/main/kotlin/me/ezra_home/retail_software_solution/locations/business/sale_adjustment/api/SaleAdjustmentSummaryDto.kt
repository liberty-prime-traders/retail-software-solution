package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.util.UUID

data class SaleAdjustmentSummaryDto(
    val saleLineId: UUID?,
    val direction: AdjustmentDirection,
    val calculatedAmount: BigDecimal,
)
