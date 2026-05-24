package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForPurchaseDto
import java.math.BigDecimal

object LocationProductForPurchaseAssembler {

    fun assemble(activeProducts: List<LocationProductDto>): List<LocationProductForPurchaseDto> {
        return activeProducts.map { locationProductDto ->
            LocationProductForPurchaseDto(
                id = locationProductDto.id,
                referenceNumber = locationProductDto.referenceNumber,
                productName = locationProductDto.productName!!,
                productGroupName = locationProductDto.productGroupName!!,
                baseUnitId = locationProductDto.baseUnitId!!,
                lastPurchasePrice = locationProductDto.lastPurchasePrice ?: BigDecimal.ZERO,
            )
        }
    }
}
