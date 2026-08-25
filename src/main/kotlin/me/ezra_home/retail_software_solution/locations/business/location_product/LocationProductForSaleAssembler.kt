package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductWithAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityFetcher
import org.springframework.stereotype.Component

@Component
class LocationProductForSaleAssembler(
    private val stockAvailabilityFetcher: StockAvailabilityFetcher,
) {

    fun assemble(activeProducts: List<LocationProductDto>): List<LocationProductWithAvailability> {
        val sellable = activeProducts.filter { it.defaultSalePrice != null }
        val availabilityByLocationProductId = stockAvailabilityFetcher.fetch(sellable.map { it.id })
        return sellable.map { locationProductDto ->
            val availability = availabilityByLocationProductId[locationProductDto.id] ?: StockAvailability.ZERO
            LocationProductWithAvailability(
                id = locationProductDto.id,
                referenceNumber = locationProductDto.referenceNumber,
                productName = locationProductDto.productName!!,
                productGroupName = locationProductDto.productGroupName!!,
                quantityOnHand = availability.quantityOnHand,
                quantityReserved = availability.quantityReserved,
                quantityAvailable = availability.quantityAvailable,
            )
        }
    }
}
