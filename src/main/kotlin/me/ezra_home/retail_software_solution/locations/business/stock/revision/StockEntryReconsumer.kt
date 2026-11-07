package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.FifoStockConsumer
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.stockEntryFifoComparator
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

// Draws from every layer with stock left, so the replacement layer must already be saved.
// Plans every consumer first and throws on any problem before a layer is changed; only then are the plans applied, in order.
@Component
@TransactionalOnLocationSchema
class StockEntryReconsumer(
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    fun reconsume(
        consumptionsOnReplacedLayer: List<StockEntryConsumption>,
        replacementStockEntryId: UUID,
        stockRevisionReferenceNumber: String
    ): List<ConsumerReplay> {
        if (consumptionsOnReplacedLayer.isEmpty()) return emptyList()
        val locationProductId = consumptionsOnReplacedLayer.first().locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
        requireActiveReplacementOfProduct(replacementStockEntryId, locationProductId)

        val fifoEntries = stockEntryRepository.findFifoEntriesForProducts(listOf(locationProductId)).sortedWith(stockEntryFifoComparator)
        val quantityBreakdown = stockBreakdownProvider.forLocationProducts(listOf(locationProductId)).getValue(locationProductId)
        val productLabel = locationProductDataFetcher.findSummaryByIds(listOf(locationProductId)).getValue(locationProductId).label
        val replayResult = ConsumerReplayer.replay(
            ReplayRequest(
                stockRevisionReferenceNumber = stockRevisionReferenceNumber,
                consumptionsOnReplacedLayer = consumptionsOnReplacedLayer,
                productBeingReplayed = ProductBeingReplayed(
                   productLabel, quantityBreakdown, fifoEntries.map { it.toFifoDto() }
                )
            )
        )
        if (replayResult.errors.isNotEmpty()) throw RtsGenericException(replayResult.errors.joinToString(" "), payload = replayResult.errors)

        var runningBalance = stockBalanceFetcher.getLatestBalances(listOf(locationProductId)).getValue(locationProductId)
        val touchedEntries = LinkedHashSet<StockEntryEntity>()
        val consumerReplays = replayResult.consumerReplayPlans.map { consumerReplayPlan ->
            val fifoConsumptionRequest = consumerReplayPlan.fifoConsumptionRequest
            val fifoConsumptionResult = FifoStockConsumer.applyTakes(
                fifoConsumptionRequest = fifoConsumptionRequest,
                plannedTakes = consumerReplayPlan.plannedTakes,
                fifoEntries = fifoEntries,
                startingBalance = runningBalance,
                quantityBreakdown = quantityBreakdown
            )
            runningBalance = fifoConsumptionResult.endingBalance
            touchedEntries.addAll(fifoConsumptionResult.touchedEntries)
            ConsumerReplay(
                locationProductId = locationProductId,
                movementType = fifoConsumptionRequest.movementType,
                documentReference = fifoConsumptionRequest.documentReference,
                detailReference = fifoConsumptionRequest.detailReference,
                oldCost = consumerReplayPlan.oldCost,
                newCost = fifoConsumptionResult.cost,
                movements = fifoConsumptionResult.movements,
            )
        }

        stockEntryRepository.saveAll(touchedEntries)
        stockMovementRepository.saveAll(consumerReplays.flatMap { it.movements })
        return consumerReplays
    }

    // Checked by id because a zero-quantity replacement is legitimate but is not among the layers with stock left.
    // Without it a replay would be covered by other layers and the replacement saved afterwards would overstate the stock.
    private fun requireActiveReplacementOfProduct(replacementStockEntryId: UUID, locationProductId: UUID) {
        val replacementStockEntry = stockEntryRepository.findById(replacementStockEntryId).orElseThrow {
            IllegalArgumentException("Replacement stock entry $replacementStockEntryId must be saved before the consumers are replayed.")
        }
        require(replacementStockEntry.status == StockEntryStatus.ACTIVE) {
            "Replacement stock entry ${replacementStockEntry.requiredReference()} is already replaced."
        }
        require(replacementStockEntry.locationProductId == locationProductId) {
            "Replacement stock entry ${replacementStockEntry.requiredReference()} belongs to another product than the consumers."
        }
    }
}
