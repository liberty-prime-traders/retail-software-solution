package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.locations.business.opening_stock.api.OpeningStockAmountsFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.QuantityBreakdownFactory
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.math.RoundingMode

@Component
class LocationProductEnricher(
    private val locationProductMapper: LocationProductMapper,
    private val unitValueFetcher: UnitValueFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val openingStockAmountsFetcher: OpeningStockAmountsFetcher,
    private val quantityBreakdownFactory: QuantityBreakdownFactory,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    fun convertToResponseDto(dtos: List<LocationProductDto>): List<LocationProductResponseDto> {
        val unitNamesById = unitValueFetcher.getUnitNamesById()
        val productIds = dtos.map { it.id }
        val balances = stockBalanceFetcher.getLatestBalances(productIds)
        val openingStockAmounts = openingStockAmountsFetcher.getAmountsByProductIds(productIds)
        val breakdownByBaseUnitId = quantityBreakdownFactory.forBaseUnits(dtos.map { it.baseUnitId })
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return dtos.map {
            val openingStock = openingStockAmounts[it.id]
            val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(it.baseUnitId)
            val quantityBreakdown = breakdownByBaseUnitId.getValue(it.baseUnitId)
            val openingStockStorageQuantity = openingStock?.let { amounts ->
                ExactQuantity.ofUserProvided(amounts.userProvidedQuantity, amounts.storageUnitsPerUserProvidedUnit)
            }
            locationProductMapper.toResponseDto(
                it,
                LocationProductContext(
                    unitName = unitNamesById.getValue(it.baseUnitId),
                    balance = BaseUnitDisplay.storageQuantityInBaseUnit(balances.getValue(it.id), storageUnitsPerBaseUnit, RoundingMode.DOWN),
                    openingStockQuantity = openingStockStorageQuantity?.toDisplayQuantity(storageUnitsPerBaseUnit, RoundingMode.DOWN),
                    openingStockUnitCost = openingStock?.unitCost,
                    balanceBreakdown = quantityBreakdown.render(ExactQuantity.ofStorage(balances.getValue(it.id))),
                    openingStockBreakdown = openingStockStorageQuantity?.let { quantity -> quantityBreakdown.render(quantity) }
                )
            )
        }
    }
}
