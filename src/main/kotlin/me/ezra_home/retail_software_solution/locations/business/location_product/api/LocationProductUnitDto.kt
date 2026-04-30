package me.ezra_home.retail_software_solution.locations.business.location_product.api

import java.math.BigDecimal
import java.util.UUID

data class LocationProductUnitRequestDto(
    val locationProductId: UUID,
    val unitId: UUID
)

data class LocationProductUnitDto(
    val locationProductId: UUID,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
)
