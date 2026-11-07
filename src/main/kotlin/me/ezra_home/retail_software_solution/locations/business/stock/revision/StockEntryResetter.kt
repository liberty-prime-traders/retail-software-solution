package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.util.UUID


@Component
@TransactionalOnLocationSchema
class StockEntryResetter(
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val netConsumptionCalculator: NetConsumptionCalculator,
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider
) {

    fun reset(stockEntryId: UUID, stockRevisionReferenceNumber: String): List<StockEntryConsumption> {
        val locationProductId = stockEntryRepository.findLocationProductIdsByIdIn(setOf(stockEntryId)).singleOrNull()
            ?: throw stockEntryNotFound(stockEntryId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
        val stockEntry = stockEntryRepository.findById(stockEntryId).orElseThrow { stockEntryNotFound(stockEntryId) }

        rejectIfReplaced(stockEntry)

        val storageQuantity = stockBalanceFetcher.getLatestBalances(listOf(locationProductId)).getValue(locationProductId)
        val quantityBreakdown = stockBreakdownProvider.forLocationProducts(listOf(locationProductId)).getValue(locationProductId)
        val entryConsumptions = netConsumptionCalculator.calculateNetConsumptions(stockEntryId)
        val restoration = StockEntryRestorer.restore(
            entryConsumptions, stockEntry,
            stockRevisionReferenceNumber,
            ExactQuantity.ofStorage(storageQuantity), quantityBreakdown
        )
        val drainMovement = StockEntryDrainer.drain(
            StockEntryDrainRequest(
                stockEntry = stockEntry,
                stockRevisionReferenceNumber = stockRevisionReferenceNumber,
                balanceBeforeDrain = restoration.endingBalance,
                quantityBreakdown = quantityBreakdown,
            )
        )

        stockEntry.status = StockEntryStatus.REPLACED
        stockEntryRepository.save(stockEntry)
        stockMovementRepository.saveAll(restoration.reversalMovements + listOfNotNull(drainMovement))
        return entryConsumptions
    }

    private fun rejectIfReplaced(stockEntry: StockEntryEntity) {
        if (stockEntry.status == StockEntryStatus.REPLACED) {
            throw RtsGenericException(
                "Replaced entries cannot be reset again",
                payload = listOf("Stock entry ${stockEntry.requiredReference()} was already replaced.")
            )
        }
    }

    private fun stockEntryNotFound(stockEntryId: UUID) = RtsGenericException("Stock entry $stockEntryId not found.")
}
