package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductWithAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityFetcher
import me.ezra_home.retail_software_solution.util.business.Decimals
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
                productName = locationProductDto.productName,
                productGroupName = locationProductDto.productGroupName,
                quantityOnHand = Decimals.Scale4.round(availability.quantityOnHand),
                quantityReserved = Decimals.Scale4.round(availability.quantityReserved),
                quantityAvailable = Decimals.Scale4.round(availability.quantityAvailable),
                defaultSalePrice = locationProductDto.defaultSalePrice!!
            )
        }
    }
}
