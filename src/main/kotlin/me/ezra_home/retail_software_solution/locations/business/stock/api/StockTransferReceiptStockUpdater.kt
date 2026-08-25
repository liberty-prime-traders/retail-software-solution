package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class StockTransferReceiptStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun findUnprocessedLineRefs(lineRefs: Collection<String>): Set<String> {
        if (lineRefs.isEmpty()) return emptySet()
        val processed = stockMovementRepository.findPresentRefs(lineRefs, MovementType.TRANSFER_IN)
        return lineRefs.toSet() - processed
    }

    fun recordTransferReceipt(event: StockTransferDispatchedEvent) {
        val productIds = event.lines.map { it.productId }.toSet()
        val identitiesByProductId = locationProductDataFetcher.findIdentitiesByProductIds(productIds)
        val locationProductIdByProductId = productIds.associateWith { productId ->
            identitiesByProductId[productId]?.locationProductId
                ?: throw RtsGenericException("No location product found for product $productId at destination")
        }

        val locationProductIds = locationProductIdByProductId.values.toList()
        val runningBalances = stockBalanceFetcher.getLatestBalances(locationProductIds).toMutableMap()

        val entriesByDispatchLineRef = event.lines.associate { line ->
            val locationProductId = locationProductIdByProductId.getValue(line.productId)
            val baseQty = Decimals.multiplyScale4(line.quantityDispatched, line.conversionFactor)
            line.dispatchLineReferenceNumber to StockEntryEntity(
                locationProductId = locationProductId,
                sourceType = StockItemSource.TRANSFER_IN,
                externalReferenceNumber = line.dispatchLineReferenceNumber,
                batchSize = baseQty,
                quantityRemaining = baseQty,
                unitCost = line.unitCost,
                priority = 0
            )
        }
        stockEntryRepository.saveAll(entriesByDispatchLineRef.values)

        val movements = event.lines.map { line ->
            val locationProductId = locationProductIdByProductId.getValue(line.productId)
            val entry = entriesByDispatchLineRef[line.dispatchLineReferenceNumber]!!
            val newBalance = (runningBalances[locationProductId] ?: BigDecimal.ZERO) + entry.batchSize
            runningBalances[locationProductId] = newBalance
            StockMovementEntity(
                stockEntryId = entry.id!!,
                locationProductId = locationProductId,
                movementType = MovementType.TRANSFER_IN,
                movedQuantity = line.quantityDispatched,
                remainingQuantity = newBalance,
                externalReferenceNumber = line.dispatchLineReferenceNumber,
                unitId = line.unitId,
                conversionFactor = line.conversionFactor
            )
        }
        stockMovementRepository.saveAll(movements)
    }
}
