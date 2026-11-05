package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.util.UUID

// The drained entries are replaced by the caller, so a drain alone loses stock.
@Component
@TransactionalOnLocationSchema
class StockEntryResetter(
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val stockEntryConsumptionFinder: StockEntryConsumptionFinder,
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider
) {

    fun reset(stockEntryIds: Collection<UUID>, stockRevisionReferenceNumber: String): List<EntryConsumption> {
        val locationProductIds = stockEntryRepository.findLocationProductIdsByIdIn(stockEntryIds)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds)
        val stockEntriesById = stockEntryRepository.findAllById(stockEntryIds).associateBy { it.id!! }
        val missingStockEntryIds = stockEntryIds.toSet() - stockEntriesById.keys
        if (missingStockEntryIds.isNotEmpty()) throw RtsGenericException("Stock entries not found: ${missingStockEntryIds.joinToString()}.")

        val movementsOnEntries = stockMovementRepository.findByStockEntryIdInOrderByCreatedOnAscIdAsc(stockEntryIds)
        StockEntryDrainer.rejectReplacedEntries(stockEntriesById.values)
        val entryConsumptions = stockEntryConsumptionFinder.netConsumptions(stockEntryIds)

        val runningBalanceByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
            .mapValues { (_, balance) -> ExactQuantity.ofStorage(balance) }
            .toMutableMap()
        val quantityBreakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(locationProductIds)

        val restorationMovements = StockEntryRestorer.restore(
            entryConsumptions, stockEntriesById, stockRevisionReferenceNumber,
            runningBalanceByLocationProductId, quantityBreakdownByLocationProductId
        )
        val drainMovements = StockEntryDrainer.drain(
            stockEntriesById.values, movementsOnEntries, stockRevisionReferenceNumber,
            runningBalanceByLocationProductId, quantityBreakdownByLocationProductId
        )

        stockEntryRepository.saveAll(stockEntriesById.values)
        stockMovementRepository.saveAll(restorationMovements + drainMovements)
        return entryConsumptions
    }
}
