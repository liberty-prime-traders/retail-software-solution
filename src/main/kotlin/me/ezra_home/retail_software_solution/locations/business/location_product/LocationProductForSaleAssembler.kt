package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductWithAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.QuantityBreakdownFactory
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.math.RoundingMode

@Component
class LocationProductForSaleAssembler(
    private val stockAvailabilityFetcher: StockAvailabilityFetcher,
    private val quantityBreakdownFactory: QuantityBreakdownFactory,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    fun assemble(sellableProducts: List<LocationProductDto>): List<LocationProductWithAvailability> {
        val availabilityByLocationProductId = stockAvailabilityFetcher.fetch(sellableProducts.map { it.id })
        val breakdownByBaseUnitId = quantityBreakdownFactory.forBaseUnits(sellableProducts.map { it.baseUnitId })
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return sellableProducts.map { locationProductDto ->
            val availability = availabilityByLocationProductId.getValue(locationProductDto.id)
            val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(locationProductDto.baseUnitId)
            val quantityBreakdown = breakdownByBaseUnitId.getValue(locationProductDto.baseUnitId)
            LocationProductWithAvailability(
                id = locationProductDto.id,
                referenceNumber = locationProductDto.referenceNumber,
                productName = locationProductDto.productName,
                productGroupName = locationProductDto.productGroupName,
                quantityOnHand = BaseUnitDisplay.storageQuantityInBaseUnit(availability.quantityOnHand, storageUnitsPerBaseUnit, RoundingMode.DOWN),
                quantityReserved = BaseUnitDisplay.storageQuantityInBaseUnit(availability.quantityReserved, storageUnitsPerBaseUnit, RoundingMode.UP),
                quantityAvailable = BaseUnitDisplay.storageQuantityInBaseUnit(availability.quantityAvailable, storageUnitsPerBaseUnit, RoundingMode.DOWN),
                defaultSalePrice = checkNotNull(locationProductDto.defaultSalePrice) {
                    "Location product ${locationProductDto.id} reached the sale assembler without a default sale price"
                },
                storageQuantityOnHand = availability.quantityOnHand,
                storageQuantityReserved = availability.quantityReserved,
                storageQuantityAvailable = availability.quantityAvailable,
                quantityOnHandBreakdown = quantityBreakdown.render(ExactQuantity.ofStorage(availability.quantityOnHand)),
                quantityAvailableBreakdown = quantityBreakdown.render(ExactQuantity.ofStorage(availability.quantityAvailable)),
            )
        }
    }
}
