package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.util.UUID

data class SaleAdjustmentCreateDto(
    val locationProductId: UUID? = null,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val value: BigDecimal,
    val adjustmentReasonId: UUID,
    val note: String? = null,
    val approvedById: UUID? = null
)
