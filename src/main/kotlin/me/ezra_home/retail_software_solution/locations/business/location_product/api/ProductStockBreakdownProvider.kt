package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.QuantityBreakdownFactory
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductStockBreakdownProvider(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val quantityBreakdownFactory: QuantityBreakdownFactory,
) {

    fun forLocationProduct(locationProductId: UUID): QuantityBreakdown {
        return forLocationProducts(listOf(locationProductId)).getValue(locationProductId)
    }

    fun forLocationProducts(locationProductIds: Collection<UUID>): Map<UUID, QuantityBreakdown> {
        if (locationProductIds.isEmpty()) return emptyMap()
        val baseUnitIdByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIds)
        val breakdownByBaseUnitId = quantityBreakdownFactory.forBaseUnits(baseUnitIdByLocationProductId.values)
        return baseUnitIdByLocationProductId.mapValues { (_, baseUnitId) -> breakdownByBaseUnitId.getValue(baseUnitId) }
    }
}
