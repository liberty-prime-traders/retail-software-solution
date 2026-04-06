package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductDto
import java.io.Serializable
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

data class LocationProductUpdateDto(
  val id: UUID,
  val defaultSalePrice: Optional<BigDecimal>? = null,
  val minStockLevel: Optional<Int>? = null
) : Serializable {

    fun applyTo(existing: LocationProductDto): LocationProductDto = existing.copy(
        defaultSalePrice = defaultSalePrice?.orElse(null) ?: existing.defaultSalePrice,
        minStockLevel = minStockLevel?.orElse(null) ?: existing.minStockLevel
    )
}
