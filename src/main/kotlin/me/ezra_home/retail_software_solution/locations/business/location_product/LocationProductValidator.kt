package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object LocationProductValidator {

  fun validateProductUpdate(dto: LocationProductUpdateDto) {
    dto.minStockLevel?.ifPresent { level ->
      if (level < 0) {
        throw RtsGenericException("Stock level cannot be negative")
      }
    }
  }
}
