package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import org.springframework.stereotype.Component

@Component
class LocationProductEnricher(
    private val locationProductMapper: LocationProductMapper,
    private val unitValueFetcher: UnitValueFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher
) {

    fun convertToResponseDto(dtos: List<LocationProductDto>): List<LocationProductResponseDto> {
        val unitNamesById = unitValueFetcher.getUnitNamesById()
        val balances = stockBalanceFetcher.getLatestBalances(dtos.map { it.id })
        return dtos.map {
            locationProductMapper.toResponseDto(it, LocationProductContext(unitNamesById[it.baseUnitId], balances[it.id]))
        }
    }
}
