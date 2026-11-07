package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.FifoStockConsumer
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementCreator
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.stockEntryFifoComparator
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

// Writes the reset and the replay of a revision; the replacement layer between them is written by
// StockReplacementLayerRecorder.
@Service
@TransactionalOnLocationSchema
class StockRevisionCommitter(
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher
) {

    fun commitReset(stockEntryResetPlan: StockEntryResetPlan, quantityBreakdown: QuantityBreakdown) {
        val locationProductId = stockEntryResetPlan.locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
        val stockEntry = stockEntryRepository.getReferenceById(stockEntryResetPlan.stockEntryId)

        val movements = stockEntryResetPlan.movementDrafts.map { movementDraft ->
            StockMovementCreator.create(movementDraft, stockEntry.id!!, locationProductId, quantityBreakdown)
        }

        stockEntry.storageQuantityRemaining = ExactQuantity.ZERO.toStorageQuantity()
        stockEntry.costRemaining = BigDecimal.ZERO
        stockEntry.status = StockEntryStatus.REPLACED
        stockEntryRepository.save(stockEntry)
        stockMovementRepository.saveAll(movements)
    }

    // Draws from every layer with stock left, so the replacement layer must already be saved.
    fun commitReplay(
        consumerReplayPlans: List<ConsumerReplayPlan>,
        replacementStockEntryId: UUID,
        quantityBreakdown: QuantityBreakdown
    ): List<ConsumerReplayDto> {
        if (consumerReplayPlans.isEmpty()) return emptyList()
        val locationProductId = consumerReplayPlans.first().fifoConsumptionRequest.locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
        requireActiveReplacementOfProduct(replacementStockEntryId, locationProductId)

        val fifoEntries = stockEntryRepository.findFifoEntriesForProducts(
            listOf(locationProductId)
        ).sortedWith(stockEntryFifoComparator)

        var runningBalance = stockBalanceFetcher.getLatestBalances(
            listOf(locationProductId)
        ).getValue(locationProductId)
        val touchedEntries = LinkedHashSet<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        val consumerReplayDtos = consumerReplayPlans.map { consumerReplayPlan ->
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
            movements.addAll(fifoConsumptionResult.movements)
            ConsumerReplayDto(
                locationProductId = locationProductId,
                movementType = fifoConsumptionRequest.movementType,
                documentReference = fifoConsumptionRequest.documentReference,
                detailReference = fifoConsumptionRequest.detailReference,
                oldCost = consumerReplayPlan.oldCost,
                newCost = fifoConsumptionResult.cost,
            )
        }

        stockEntryRepository.saveAll(touchedEntries)
        stockMovementRepository.saveAll(movements)
        return consumerReplayDtos
    }

    // Checked by id because a zero-quantity replacement is legitimate but is not among the layers with stock left.
    // Without it a replay would be covered by other layers and the replacement saved afterwards would overstate the
    // stock.
    private fun requireActiveReplacementOfProduct(replacementStockEntryId: UUID, locationProductId: UUID) {
        val replacementStockEntry = stockEntryRepository.findById(replacementStockEntryId).orElseThrow {
            IllegalArgumentException(
                "Replacement stock entry $replacementStockEntryId must be saved before the consumers are replayed."
            )
        }
        require(replacementStockEntry.status == StockEntryStatus.ACTIVE) {
            "Replacement stock entry ${replacementStockEntry.requiredReference()} is already replaced."
        }
        require(replacementStockEntry.locationProductId == locationProductId) {
            "Replacement stock entry ${replacementStockEntry.requiredReference()} " +
                "belongs to another product than the consumers."
        }
    }
}
