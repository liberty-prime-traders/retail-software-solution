package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import java.math.BigDecimal
import java.util.UUID

data class SaleDiscountSummaryDto(
    val saleLineId: UUID?,
    val calculatedAmount: BigDecimal,
)
