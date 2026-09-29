package me.ezra_home.retail_software_solution.locations.business.location_product.api

import java.math.BigDecimal
import java.util.UUID

data class LocationProductWithAvailability(
    val id: UUID,
    val referenceNumber: String,
    val productName: String,
    val productGroupName: String,
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val quantityAvailable: BigDecimal,
)
