package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

// unitPrice is derived for display and never stored; discounts and adjustments work from the gross lineTotal
interface ProductLineWithPrice {
    val locationProductId: UUID
    val userProvidedQuantity: BigDecimal
    val lineTotal: BigDecimal

    val unitPrice: BigDecimal
        get() = if (userProvidedQuantity.signum() == 0) BigDecimal.ZERO else Decimals.Scale4.divide(lineTotal, userProvidedQuantity)
}
