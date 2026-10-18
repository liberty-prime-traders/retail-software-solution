package me.ezra_home.retail_software_solution.locations.business.location_product.api

import java.util.UUID

data class LocationProductUnitRequestDto(
    val locationProductId: UUID,
    val unitId: UUID
)
