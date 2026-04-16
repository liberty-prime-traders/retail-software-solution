package me.ezra_home.retail_software_solution.locations.business.location_product

import java.math.BigDecimal

data class LocationProductContext(
    val unitName: String?,
    val balance: BigDecimal? = null
)
