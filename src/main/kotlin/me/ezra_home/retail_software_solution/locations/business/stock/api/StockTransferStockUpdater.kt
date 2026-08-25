package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

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
        val fifoEntriesByLocationProductId = loadFifoEntriesByLocationProductId(locationProductIds)
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val productSummariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds.toSet())

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        dispatchLineRequests.groupBy { it.locationProductId }.forEach { (locationProductId, requestsForProduct) ->
            val productLabel = productSummariesByLocationProductId[locationProductId]?.label
                ?: locationProductId.toString()
            var runningBalance = balancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO
            requestsForProduct.forEach { dispatchLineRequest ->
                runningBalance = consumeStockForLine(
                    dispatchLineRequest = dispatchLineRequest,
                    fifoEntries = fifoEntriesByLocationProductId[locationProductId].orEmpty(),
                    startingBalance = runningBalance,
                    productLabel = productLabel,
                    modifiedEntries = modifiedEntries,
                    movements = movements
                )
            }
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(movements)
    }

    private fun consumeStockForLine(
        dispatchLineRequest: StockTransferDispatchLineStockRequest,
        fifoEntries: List<StockEntryEntity>,
        startingBalance: BigDecimal,
        productLabel: String,
        modifiedEntries: MutableList<StockEntryEntity>,
        movements: MutableList<StockMovementEntity>
    ): BigDecimal {
        var runningBalance = startingBalance
        var unsatisfiedQuantity = dispatchLineRequest.baseQuantity
        for (stockEntry in fifoEntries) {
            if (unsatisfiedQuantity <= BigDecimal.ZERO) break
            val takenQuantityInBaseUnit = unsatisfiedQuantity.min(stockEntry.quantityRemaining)
            stockEntry.quantityRemaining = stockEntry.quantityRemaining.subtract(takenQuantityInBaseUnit)
            modifiedEntries.add(stockEntry)
            runningBalance = runningBalance.subtract(takenQuantityInBaseUnit)
            movements.add(
                StockMovementEntity(
                    stockEntryId = stockEntry.id!!,
                    locationProductId = dispatchLineRequest.locationProductId,
                    movementType = MovementType.TRANSFER_OUT,
                    movedQuantity = Decimals.divideScale4(takenQuantityInBaseUnit, dispatchLineRequest.conversionFactor),
                    remainingQuantity = runningBalance,
                    externalReferenceNumber = dispatchLineRequest.dispatchLineRef,
                    unitId = dispatchLineRequest.unitId,
                    conversionFactor = dispatchLineRequest.conversionFactor
                )
            )
            unsatisfiedQuantity = unsatisfiedQuantity.subtract(takenQuantityInBaseUnit)
        }
        throwIfNotFulfilled(dispatchLineRequest, unsatisfiedQuantity, productLabel)
        return runningBalance
    }

    private fun throwIfNotFulfilled(
        dispatchLineRequest: StockTransferDispatchLineStockRequest,
        unsatisfiedQuantity: BigDecimal,
        productLabel: String
    ) {
        if (unsatisfiedQuantity <= BigDecimal.ZERO) return
        val formattedAvailable = Decimals.stripZeroesAndRound(dispatchLineRequest.baseQuantity.subtract(unsatisfiedQuantity))
        val formattedRequested = Decimals.stripZeroesAndRound(dispatchLineRequest.baseQuantity)
        throw RtsGenericException(
            "Insufficient stock for $productLabel. Available: $formattedAvailable, Requested: $formattedRequested"
        )
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
        ).toMutableMap()

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val newMovements = mutableListOf<StockMovementEntity>()
        transferOutMovements.groupBy { it.locationProductId }.forEach { (locationProductId, stockMovementsForProduct) ->
            var runningBalance = balancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO
            stockMovementsForProduct.forEach { stockMovement ->
                val stockEntry = stockEntriesById[stockMovement.stockEntryId]!!
                val restoredBaseQuantity = Decimals.multiplyScale4(stockMovement.movedQuantity, stockMovement.conversionFactor)
                stockEntry.quantityRemaining = stockEntry.quantityRemaining.add(restoredBaseQuantity)
                modifiedEntries.add(stockEntry)
                runningBalance = runningBalance.add(restoredBaseQuantity)
                newMovements.add(
                    StockMovementEntity(
                        stockEntryId = stockMovement.stockEntryId,
                        locationProductId = locationProductId,
                        movementType = MovementType.TRANSFER_CANCELLED,
                        movedQuantity = stockMovement.movedQuantity,
                        remainingQuantity = runningBalance,
                        externalReferenceNumber = stockMovement.externalReferenceNumber,
                        unitId = stockMovement.unitId,
                        conversionFactor = stockMovement.conversionFactor
                    )
                )
            }
            balancesByLocationProductId[locationProductId] = runningBalance
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(newMovements)
    }

    private fun loadFifoEntriesByLocationProductId(locationProductIds: List<UUID>): Map<UUID, List<StockEntryEntity>> =
        stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, stockEntries) -> stockEntries.sortedWith(stockEntryFifoComparator) }

}
