package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import java.math.BigDecimal
import java.util.UUID

data class SaleDiscountResponseDto(
    val id: UUID,
    val saleLineId: UUID?,
    val calculationMethod: CalculationMethod,
    val value: BigDecimal,
    val calculatedAmount: BigDecimal,
    val description: String,
    val approvedById: UUID?,
    val approvedBy: String?
)
