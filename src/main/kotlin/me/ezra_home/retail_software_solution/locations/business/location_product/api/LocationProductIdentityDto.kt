package me.ezra_home.retail_software_solution.locations.business.location_product.api

import java.util.UUID

data class LocationProductIdentityDto(
    val locationProductId: UUID,
    val orgProductId: UUID
)
