package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductDto
import me.ezra_home.retail_software_solution.util.business.Money
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class LocationProductUpdateDto(
  val id: UUID,
  val defaultSalePrice: Optional<Money>? = null,
  val minStockLevel: Optional<Int>? = null
) : Serializable {

    fun applyTo(existing: LocationProductDto): LocationProductDto = existing.copy(
        defaultSalePrice = if (defaultSalePrice != null) defaultSalePrice.orElse(null)?.toBigDecimal() else existing.defaultSalePrice,
        minStockLevel = if (minStockLevel != null) minStockLevel.orElse(null) else existing.minStockLevel
    )
}
