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
class SaleStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun consumeStock(saleLineStockRequests: List<SaleLineStockRequest>, saleRefNumber: String) {
        if (saleLineStockRequests.isEmpty()) return
        val locationProductIds = saleLineStockRequests.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByLocationProductId = loadFifoEntriesByLocationProductId(locationProductIds)
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val productSummariesByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(saleLineStockRequests.map { it.locationProductId }.toSet())

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        saleLineStockRequests.forEach { saleLineStockRequest ->
            val productLabel = productSummariesByLocationProductId[saleLineStockRequest.locationProductId]?.label
                ?: saleLineStockRequest.locationProductId.toString()
            consumeStockForLine(
                saleLineStockRequest = saleLineStockRequest,
                fifoEntries = fifoEntriesByLocationProductId[saleLineStockRequest.locationProductId].orEmpty(),
                startingBalance = balancesByLocationProductId[saleLineStockRequest.locationProductId] ?: BigDecimal.ZERO,
                saleRefNumber = saleRefNumber,
                productLabel = productLabel,
                modifiedEntries = modifiedEntries,
                movements = movements,
            )
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(movements)
    }

    private fun loadFifoEntriesByLocationProductId(locationProductIds: List<UUID>): Map<UUID, List<StockEntryEntity>> {
        return stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
            .groupBy { it.locationProductId }
            .mapValues { (_, stockEntries) -> stockEntries.sortedWith(stockEntryFifoComparator) }
    }

    private fun consumeStockForLine(
        saleLineStockRequest: SaleLineStockRequest,
        fifoEntries: List<StockEntryEntity>,
        startingBalance: BigDecimal,
        saleRefNumber: String,
        productLabel: String,
        modifiedEntries: MutableList<StockEntryEntity>,
        movements: MutableList<StockMovementEntity>,
    ) {
        var runningBalance = startingBalance
        var unsatisfiedQuantity = saleLineStockRequest.baseQuantity
        for (stockEntry in fifoEntries) {
            if (unsatisfiedQuantity <= BigDecimal.ZERO) break
            val takenQuantityInBaseUnit = unsatisfiedQuantity.min(stockEntry.quantityRemaining)
            stockEntry.quantityRemaining = stockEntry.quantityRemaining.subtract(takenQuantityInBaseUnit)
            modifiedEntries.add(stockEntry)
            runningBalance = runningBalance.subtract(takenQuantityInBaseUnit)
            val conversionRatio = saleLineStockRequest.conversionRatio
            movements.add(
                StockMovementEntity(
                    stockEntryId = stockEntry.id!!,
                    locationProductId = saleLineStockRequest.locationProductId,
                    movementType = MovementType.SALE,
                    movedQuantity = conversionRatio.invert().applyTo(takenQuantityInBaseUnit),
                    remainingQuantity = runningBalance,
                    externalReferenceNumber = saleRefNumber,
                    unitId = saleLineStockRequest.unitId,
                    conversionNumerator = conversionRatio.numerator,
                    conversionDenominator = conversionRatio.denominator,
                )
            )
            unsatisfiedQuantity = unsatisfiedQuantity.subtract(takenQuantityInBaseUnit)
        }
        throwIfNotFulfilled(saleLineStockRequest, unsatisfiedQuantity, productLabel)
    }

    private fun throwIfNotFulfilled(
        saleLineStockRequest: SaleLineStockRequest,
        unsatisfiedQuantity: BigDecimal,
        productLabel: String
    ) {
        if (unsatisfiedQuantity <= BigDecimal.ZERO) return
        val formattedAvailable = Decimals.stripZeroesAndRound(saleLineStockRequest.baseQuantity.subtract(unsatisfiedQuantity))
        val formattedRequested = Decimals.stripZeroesAndRound(saleLineStockRequest.baseQuantity)
        throw RtsGenericException(
            "Insufficient stock for $productLabel. Available: $formattedAvailable, Requested: $formattedRequested"
        )
    }

    fun restoreStock(saleRefNumber: String) {
        val saleMovements = stockMovementRepository
            .findByExternalReferenceNumberAndMovementType(saleRefNumber, MovementType.SALE)
        if (saleMovements.isEmpty()) return
        val saleMovementsByLocationProductId = saleMovements.groupBy { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, saleMovementsByLocationProductId.keys)
        val stockEntriesById = loadStockEntriesByMovementId(saleMovements)
        val runningBalancesByLocationProductId = stockBalanceFetcher.getLatestBalances(
            saleMovementsByLocationProductId.keys.toList()
        ).toMutableMap()

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val newMovements = mutableListOf<StockMovementEntity>()
        saleMovementsByLocationProductId.forEach { (locationProductId, saleMovementsForProduct) ->
            val endingBalance = restoreStockForProduct(
                locationProductId = locationProductId,
                saleMovementsForProduct = saleMovementsForProduct,
                stockEntriesById = stockEntriesById,
                startingBalance = runningBalancesByLocationProductId[locationProductId] ?: BigDecimal.ZERO,
                saleRefNumber = saleRefNumber,
                modifiedEntries = modifiedEntries,
                newMovements = newMovements,
            )
            runningBalancesByLocationProductId[locationProductId] = endingBalance
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(newMovements)
    }

    private fun loadStockEntriesByMovementId(
        saleMovements: List<StockMovementEntity>,
    ): Map<UUID, StockEntryEntity> =
        stockEntryRepository.findAllById(saleMovements.map { it.stockEntryId }.distinct())
            .associateBy { it.id!! }

    private fun restoreStockForProduct(
        locationProductId: UUID,
        saleMovementsForProduct: List<StockMovementEntity>,
        stockEntriesById: Map<UUID, StockEntryEntity>,
        startingBalance: BigDecimal,
        saleRefNumber: String,
        modifiedEntries: MutableList<StockEntryEntity>,
        newMovements: MutableList<StockMovementEntity>,
    ): BigDecimal {
        var runningBalance = startingBalance
        saleMovementsForProduct.forEach { saleMovement ->
            val stockEntry = stockEntriesById[saleMovement.stockEntryId]!!
            val conversionRatio = saleMovement.conversionRatio()
            val restoredBaseQuantity = conversionRatio.applyTo(saleMovement.movedQuantity)
            stockEntry.quantityRemaining = stockEntry.quantityRemaining.add(restoredBaseQuantity)
            modifiedEntries.add(stockEntry)
            runningBalance = runningBalance.add(restoredBaseQuantity)
            newMovements.add(
                StockMovementEntity(
                    stockEntryId = saleMovement.stockEntryId,
                    locationProductId = locationProductId,
                    movementType = MovementType.SALE_VOID,
                    movedQuantity = saleMovement.movedQuantity,
                    remainingQuantity = runningBalance,
                    externalReferenceNumber = saleRefNumber,
                    unitId = saleMovement.unitId,
                    conversionNumerator = conversionRatio.numerator,
                    conversionDenominator = conversionRatio.denominator,
                )
            )
        }
        return runningBalance
    }
}
