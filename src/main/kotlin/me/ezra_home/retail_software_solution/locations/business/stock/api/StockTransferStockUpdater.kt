package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.FifoStockConsumer
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class StockTransferStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun findUnrestoredLineRefs(lineRefs: Collection<String>): Set<String> {
        if (lineRefs.isEmpty()) return emptySet()
        val consumedLineRefs = stockMovementRepository.findPresentRefs(lineRefs, MovementType.TRANSFER_OUT)
        val restoredLineRefs = stockMovementRepository.findPresentRefs(lineRefs, MovementType.TRANSFER_CANCELLED)
        return consumedLineRefs - restoredLineRefs
    }

    fun consumeStockForDispatch(dispatchLineRequests: List<StockTransferDispatchLineStockRequest>) {
        if (dispatchLineRequests.isEmpty()) return
        val locationProductIds = dispatchLineRequests.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByLocationProductId = FifoStockConsumer.groupInFifoOrder(
            stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
        )
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val productSummariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds.toSet())

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        dispatchLineRequests.groupBy { it.locationProductId }.forEach { (locationProductId, requestsForProduct) ->
            val productLabel = productSummariesByLocationProductId[locationProductId]?.label
                ?: locationProductId.toString()
            var runningBalance = balancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO
            requestsForProduct.forEach { dispatchLineRequest ->
                val fifoConsumptionResult = FifoStockConsumer.consume(
                    fifoConsumptionRequest = FifoConsumptionRequest(
                        locationProductId = locationProductId,
                        baseQuantity = dispatchLineRequest.baseQuantity,
                        unitId = dispatchLineRequest.unitId,
                        conversionRatio = dispatchLineRequest.conversionRatio,
                        movementType = MovementType.TRANSFER_OUT,
                        externalReferenceNumber = dispatchLineRequest.dispatchLineRef,
                    ),
                    fifoEntries = fifoEntriesByLocationProductId[locationProductId].orEmpty(),
                    startingBalance = runningBalance,
                    productLabel = productLabel,
                )
                runningBalance = fifoConsumptionResult.endingBalance
                modifiedEntries.addAll(fifoConsumptionResult.touchedEntries)
                movements.addAll(fifoConsumptionResult.movements)
            }
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(movements)
    }

    fun restoreStockForLines(dispatchLineRefs: Collection<String>) {
        if (dispatchLineRefs.isEmpty()) return
        val transferOutMovements = stockMovementRepository
            .findByExternalReferenceNumberInAndMovementType(dispatchLineRefs, MovementType.TRANSFER_OUT)
        if (transferOutMovements.isEmpty()) return

        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, transferOutMovements.map { it.locationProductId }.toSet())

        val stockEntriesById = stockEntryRepository
            .findAllById(transferOutMovements.map { it.stockEntryId }.distinct())
            .associateBy { it.id!! }
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(
            transferOutMovements.map { it.locationProductId }.distinct()
        )

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val newMovements = mutableListOf<StockMovementEntity>()
        transferOutMovements.groupBy { it.locationProductId }.forEach { (locationProductId, stockMovementsForProduct) ->
            val fifoRestorationResult = FifoStockConsumer.restore(
                movementsToReverse = stockMovementsForProduct,
                stockEntriesById = stockEntriesById,
                startingBalance = balancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO,
                reversalType = MovementType.TRANSFER_CANCELLED,
            )
            modifiedEntries.addAll(fifoRestorationResult.touchedEntries)
            newMovements.addAll(fifoRestorationResult.movements)
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(newMovements)
    }
}
