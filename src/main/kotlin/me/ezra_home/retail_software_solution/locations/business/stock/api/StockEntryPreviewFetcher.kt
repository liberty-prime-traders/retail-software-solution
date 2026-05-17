package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockEntryPreviewFetcher(private val stockEntryRepository: StockEntryRepository) {

    fun fetchAvailableEntriesByProductIds(locationProductIds: Collection<UUID>): Map<UUID, List<StockEntryPreview>> {
        if (locationProductIds.isEmpty()) return emptyMap()
        return stockEntryRepository
            .findByLocationProductIdInAndQuantityRemainingGreaterThan(locationProductIds, BigDecimal.ZERO)
            .map { it.toPreview() }
            .groupBy { it.locationProductId }
            .mapValues { (_, entries) -> entries.sortedWith(fifoComparator()) }
    }

    private fun StockEntryEntity.toPreview(): StockEntryPreview = StockEntryPreview(
        id = id!!,
        locationProductId = locationProductId,
        externalReferenceNumber = externalReferenceNumber,
        priority = priority,
        availableBaseQty = quantityRemaining,
        unitCost = unitCost
    )

    // FIFO: priority ascending, nulls last.
    private fun fifoComparator(): Comparator<StockEntryPreview> =
        compareBy(nullsLast()) { it.priority }
}
