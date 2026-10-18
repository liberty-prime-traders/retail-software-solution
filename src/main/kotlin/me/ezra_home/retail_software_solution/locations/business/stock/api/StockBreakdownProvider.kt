package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class StockBreakdownProvider(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val quantityBreakdownFactory: QuantityBreakdownFactory,
) {

    fun forLocationProducts(locationProductIds: Collection<UUID>): Map<UUID, QuantityBreakdown> {
        if (locationProductIds.isEmpty()) return emptyMap()
        val baseUnitIdByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIds)
        val breakdownByBaseUnitId = quantityBreakdownFactory.forBaseUnits(baseUnitIdByLocationProductId.values)
        return baseUnitIdByLocationProductId.mapValues { (_, baseUnitId) -> breakdownByBaseUnitId.getValue(baseUnitId) }
    }
}
