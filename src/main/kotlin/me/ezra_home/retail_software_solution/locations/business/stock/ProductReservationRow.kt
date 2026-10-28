package me.ezra_home.retail_software_solution.locations.business.stock

import java.math.BigDecimal
import java.util.UUID

data class ProductReservationRow(
    val locationProductId: UUID,
    val saleId: UUID,
    val storageQuantityReserved: BigDecimal,
)
