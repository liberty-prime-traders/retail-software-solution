package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import java.math.BigDecimal
import java.util.UUID

data class DiscountAmount(
    val locationProductId: UUID?,
    val calculatedAmount: BigDecimal,
    val isLineLevelDiscount: Boolean = locationProductId != null
)
