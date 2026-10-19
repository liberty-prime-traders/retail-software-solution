package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.FifoConsumptionRequest
import me.ezra_home.retail_software_solution.locations.business.stock.FifoStockConsumer
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockInboundLayers
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockAdjustmentStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider,
    private val stockAvailabilityValidator: StockAvailabilityValidator,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun increaseStock(stockAdjustmentIncreaseStockRequest: StockAdjustmentIncreaseStockRequest) {
        val locationProductId = stockAdjustmentIncreaseStockRequest.locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        val inboundLayerSpec = InboundLayerSpec(
            locationProductId = locationProductId,
            sourceType = StockItemSource.STOCK_ADJUSTMENT,
            movementType = MovementType.STOCK_ADJUSTMENT_INCREASE,
            documentReference = stockAdjustmentIncreaseStockRequest.documentReference,
            detailReference = null,
            storageQuantity = stockAdjustmentIncreaseStockRequest.storageQuantity,
            batchCost = stockAdjustmentIncreaseStockRequest.totalCost,
            userProvidedQuantity = stockAdjustmentIncreaseStockRequest.userProvidedQuantity,
            unitId = stockAdjustmentIncreaseStockRequest.unitId,
            storageUnitsPerUnit = stockAdjustmentIncreaseStockRequest.storageUnitsPerUnit,
            adjustmentDetails = stockAdjustmentIncreaseStockRequest.adjustmentDetails,
        )
        val balanceAfterMovement = ExactQuantity.ofStorage(
            stockBalanceFetcher.getLatestBalances(listOf(locationProductId))[locationProductId] ?: BigDecimal.ZERO
        ) + inboundLayerSpec.storageQuantity
        val stockEntry = stockEntryRepository.save(StockInboundLayers.newLayer(inboundLayerSpec))
        val quantityBreakdown = stockBreakdownProvider.forLocationProducts(listOf(locationProductId)).getValue(locationProductId)
        stockMovementRepository.save(
            StockInboundLayers.inboundMovement(inboundLayerSpec, stockEntry, balanceAfterMovement, quantityBreakdown)
        )
    }

    fun latestLayerUnitCostPerStorageUnit(locationProductId: UUID): BigDecimal? =
        stockEntryRepository.findFirstByLocationProductIdOrderByCreatedOnDesc(locationProductId)?.unitCost

    fun decreaseStock(stockAdjustmentDecreaseStockRequest: StockAdjustmentDecreaseStockRequest): StockAdjustmentConsumedCost {
        val locationProductId = stockAdjustmentDecreaseStockRequest.locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        val productLabel = stockAdjustmentDecreaseStockRequest.productLabel
        stockAvailabilityValidator.guardSufficientStock(
            mapOf(locationProductId to stockAdjustmentDecreaseStockRequest.storageQuantity),
            mapOf(locationProductId to productLabel),
        )
        val fifoConsumptionResult = FifoStockConsumer.consume(
            fifoConsumptionRequest = FifoConsumptionRequest(
                locationProductId = locationProductId,
                storageQuantity = stockAdjustmentDecreaseStockRequest.storageQuantity,
                userProvidedQuantity = stockAdjustmentDecreaseStockRequest.userProvidedQuantity,
                unitId = stockAdjustmentDecreaseStockRequest.unitId,
                storageUnitsPerUnit = stockAdjustmentDecreaseStockRequest.storageUnitsPerUnit,
                movementType = MovementType.STOCK_ADJUSTMENT_DECREASE,
                documentReference = stockAdjustmentDecreaseStockRequest.documentReference,
                detailReference = null,
                adjustmentDetails = stockAdjustmentDecreaseStockRequest.adjustmentDetails,
            ),
            fifoEntries = FifoStockConsumer.groupInFifoOrder(
                stockEntryRepository.findFifoEntriesForProducts(listOf(locationProductId))
            )[locationProductId].orEmpty(),
            startingBalance = stockBalanceFetcher.getLatestBalances(listOf(locationProductId))[locationProductId] ?: BigDecimal.ZERO,
            quantityBreakdown = stockBreakdownProvider.forLocationProducts(listOf(locationProductId)).getValue(locationProductId),
            productLabel = productLabel,
        )
        stockEntryRepository.saveAll(fifoConsumptionResult.touchedEntries)
        stockMovementRepository.saveAll(fifoConsumptionResult.movements)
        return StockAdjustmentConsumedCost(fifoConsumptionResult.cost)
    }
}
