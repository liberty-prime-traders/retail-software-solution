package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.util.business.ConversionRatio
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
        val orgProductIds = event.lines.map { it.orgProductId }.toSet()
        val identitiesByOrgProductId = locationProductDataFetcher.findIdentitiesByOrgProductIds(orgProductIds)
        val locationProductIdByOrgProductId = orgProductIds.associateWith { orgProductId ->
            identitiesByOrgProductId[orgProductId]?.locationProductId
                ?: throw RtsGenericException("No location product found for product $orgProductId at destination")
        }

        val locationProductIds = locationProductIdByOrgProductId.values.toList()
        val runningBalances = stockBalanceFetcher.getLatestBalances(locationProductIds).toMutableMap()

        val entriesByDispatchLineRef = event.lines.associate { line ->
            val locationProductId = locationProductIdByOrgProductId.getValue(line.orgProductId)
            val ratio = ConversionRatio(line.conversionNumerator, line.conversionDenominator)
            val baseQty = ratio.applyTo(line.quantityDispatched)
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
            val locationProductId = locationProductIdByOrgProductId.getValue(line.orgProductId)
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
                conversionNumerator = line.conversionNumerator,
                conversionDenominator = line.conversionDenominator
            )
        }
        stockMovementRepository.saveAll(movements)
    }
}
