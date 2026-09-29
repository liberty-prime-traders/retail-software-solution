package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal

object LocationProductValidator {

  fun validateProductUpdate(dto: LocationProductUpdateDto) {
    dto.defaultSalePrice?.ifPresent { price ->
      if (price < BigDecimal.ZERO) {
        throw RtsGenericException("Price cannot be negative")
      }
    }

    dto.minStockLevel?.ifPresent { level ->
      if (level < 0) {
        throw RtsGenericException("Stock level cannot be negative")
      }
    }
  }
}
