package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockBalanceFetcher(private val stockEntryRepository: StockEntryRepository) {

    fun getFifoEntriesByProduct(locationProductIds: List<UUID>): Map<UUID, List<StockEntryFifoDto>> =
        stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, entries) ->
                entries.sortedWith(stockEntryFifoComparator).map { entry ->
                    StockEntryFifoDto(
                        locationProductId = entry.locationProductId,
                        unitCost = entry.unitCost,
                        quantityRemaining = entry.quantityRemaining
                    )
                }
            }

    fun getLatestBalances(locationProductIds: Collection<UUID>): Map<UUID, BigDecimal> {
        val balancesByProduct = stockEntryRepository.sumRemainingByProducts(locationProductIds)
            .associateBy { it.getLocationProductId() }
        return locationProductIds.associateWith {
            balancesByProduct[it]?.getRemainingQuantity() ?: BigDecimal.ZERO
        }
    }
}
