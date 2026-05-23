package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

interface ProductLineWithPrice {
    val locationProductId: UUID
    val quantity: BigDecimal
    val unitPrice: BigDecimal

    val lineTotal: BigDecimal
        get() = Decimals.multiplyScale4(quantity, unitPrice)
}
