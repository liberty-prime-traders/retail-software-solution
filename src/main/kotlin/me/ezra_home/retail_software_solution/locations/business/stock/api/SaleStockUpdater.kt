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
class SaleStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun consumeStock(saleLineStockRequests: List<SaleLineStockRequest>, saleRefNumber: String): List<SaleLineConsumedCost> {
        if (saleLineStockRequests.isEmpty()) return emptyList()
        val locationProductIds = saleLineStockRequests.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByLocationProductId = FifoStockConsumer.groupInFifoOrder(
            stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
        )
        val runningBalancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds).toMutableMap()
        val productSummariesByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(saleLineStockRequests.map { it.locationProductId }.toSet())

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        val saleLineConsumedCosts = saleLineStockRequests.map { saleLineStockRequest ->
            val locationProductId = saleLineStockRequest.locationProductId
            val fifoConsumptionResult = FifoStockConsumer.consume(
                fifoConsumptionRequest = FifoConsumptionRequest(
                    locationProductId = locationProductId,
                    baseQuantity = saleLineStockRequest.baseQuantity,
                    unitId = saleLineStockRequest.unitId,
                    conversionRatio = saleLineStockRequest.conversionRatio,
                    movementType = MovementType.SALE,
                    externalReferenceNumber = saleRefNumber,
                ),
                fifoEntries = fifoEntriesByLocationProductId[locationProductId].orEmpty(),
                startingBalance = runningBalancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO,
                productLabel = productSummariesByLocationProductId[locationProductId]?.label ?: locationProductId.toString(),
            )
            runningBalancesByLocationProductId[locationProductId] = fifoConsumptionResult.endingBalance
            modifiedEntries.addAll(fifoConsumptionResult.touchedEntries)
            movements.addAll(fifoConsumptionResult.movements)
            SaleLineConsumedCost(saleLineStockRequest.saleLineId, fifoConsumptionResult.cost)
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(movements)
        return saleLineConsumedCosts
    }

    fun restoreStock(saleRefNumber: String) {
        val saleMovements = stockMovementRepository
            .findByExternalReferenceNumberAndMovementType(saleRefNumber, MovementType.SALE)
        if (saleMovements.isEmpty()) return
        val saleMovementsByLocationProductId = saleMovements.groupBy { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, saleMovementsByLocationProductId.keys)
        val stockEntriesById = stockEntryRepository.findAllById(saleMovements.map { it.stockEntryId }.distinct())
            .associateBy { it.id!! }
        val runningBalancesByLocationProductId = stockBalanceFetcher.getLatestBalances(
            saleMovementsByLocationProductId.keys.toList()
        ).toMutableMap()

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val newMovements = mutableListOf<StockMovementEntity>()
        saleMovementsByLocationProductId.forEach { (locationProductId, saleMovementsForProduct) ->
            val fifoRestorationResult = FifoStockConsumer.restore(
                movementsToReverse = saleMovementsForProduct,
                stockEntriesById = stockEntriesById,
                startingBalance = runningBalancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO,
                reversalType = MovementType.SALE_VOID,
            )
            runningBalancesByLocationProductId[locationProductId] = fifoRestorationResult.endingBalance
            modifiedEntries.addAll(fifoRestorationResult.touchedEntries)
            newMovements.addAll(fifoRestorationResult.movements)
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(newMovements)
    }
}
