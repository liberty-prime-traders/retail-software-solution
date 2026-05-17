package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockEntryFetcher(private val stockEntryRepository: StockEntryRepository) {

    fun fetchAvailableEntriesByProductIds(locationProductIds: Collection<UUID>): Map<UUID, List<StockEntryDto>> {
        if (locationProductIds.isEmpty()) return emptyMap()
        return stockEntryRepository
            .findFifoEntriesForProducts(locationProductIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, entries) ->
                entries.sortedWith(stockEntryFifoComparator)
                    .mapIndexed { index, entity -> entity.toDto(order = index) }
            }
    }

    private fun StockEntryEntity.toDto(order: Int): StockEntryDto = StockEntryDto(
        id = id!!,
        locationProductId = locationProductId,
        externalReferenceNumber = externalReferenceNumber,
        order = order,
        availableBaseQty = quantityRemaining,
        unitCost = unitCost
    )
}
