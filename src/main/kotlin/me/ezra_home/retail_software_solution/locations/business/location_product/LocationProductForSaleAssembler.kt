package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFetcher
import org.springframework.stereotype.Component

@Component
class LocationProductForSaleAssembler(
    private val stockEntryFetcher: StockEntryFetcher,
) {

    fun assemble(activeProducts: List<LocationProductDto>): List<LocationProductForSaleDto> {
        val sellable = activeProducts.filter { it.defaultSalePrice != null }
        val entriesByProductId = stockEntryFetcher.fetchAvailableEntriesByProductIds(sellable.map { it.id })
        return sellable.map { locationProductDto ->
            LocationProductForSaleDto(
                id = locationProductDto.id,
                referenceNumber = locationProductDto.referenceNumber,
                productName = locationProductDto.productName!!,
                productGroupName = locationProductDto.productGroupName!!,
                defaultSalePrice = locationProductDto.defaultSalePrice!!,
                stockBatches = entriesByProductId[locationProductDto.id].orEmpty()
            )
        }
    }
}
