package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import org.springframework.stereotype.Component

@Component
class LocationProductEnricher(
    private val locationProductMapper: LocationProductMapper,
    private val unitValueService: UnitValueService,
    private val stockBalanceFetcher: StockBalanceFetcher
) {

    fun provideMappingContextForEntities(entities: List<LocationProductEntity>): List<LocationProductResponseDto> {
        return provideMappingContext(entities.map { locationProductMapper.toDomainDto(it) })
    }

    fun provideMappingContext(dtos: List<LocationProductDto>): List<LocationProductResponseDto> {
        val unitNamesById = unitValueService.getUnitNamesById()
        val balances = stockBalanceFetcher.getLatestBalances(dtos.map { it.id })
        return dtos.map {
            locationProductMapper.toResponseDto(it, LocationProductContext(unitNamesById[it.baseUnitId], balances[it.id]))
        }
    }
}
