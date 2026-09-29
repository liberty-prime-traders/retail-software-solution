package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockAmountsFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import org.springframework.stereotype.Component

@Component
class LocationProductEnricher(
    private val locationProductMapper: LocationProductMapper,
    private val unitValueFetcher: UnitValueFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val openingStockAmountsFetcher: OpeningStockAmountsFetcher
) {

    fun convertToResponseDto(dtos: List<LocationProductDto>): List<LocationProductResponseDto> {
        val unitNamesById = unitValueFetcher.getUnitNamesById()
        val productIds = dtos.map { it.id }
        val balances = stockBalanceFetcher.getLatestBalances(productIds)
        val openingStockAmounts = openingStockAmountsFetcher.getAmountsByProductIds(productIds)
        return dtos.map {
            val openingStock = openingStockAmounts[it.id]
            locationProductMapper.toResponseDto(
                it,
                LocationProductContext(
                    unitName = unitNamesById.getValue(it.baseUnitId),
                    balance = balances.getValue(it.id),
                    openingStockQuantity = openingStock?.quantity,
                    openingStockUnitCost = openingStock?.unitCost
                )
            )
        }
    }
}
