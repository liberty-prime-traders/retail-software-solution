package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSourceService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class StockUpdaterForPurchaseDelivery(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockItemSourceService: StockItemSourceService
) {

    fun recordPurchaseDelivery(event: PurchaseDeliveredEvent) {
        val sourceTypeId = stockItemSourceService.findSourceId(StockItemSource.PURCHASE)
        val entriesByLineId = event.lines.associate { line ->
            line.deliveryLineId to StockEntryEntity(
                purchaseDeliveryLineId = line.deliveryLineId,
                locationProductId = line.locationProductId,
                sourceTypeId = sourceTypeId,
                batchSize = line.quantityDelivered,
                quantityRemaining = line.quantityDelivered,
                unitCost = line.unitCost,
                priority = 0
            )
        }
        stockEntryRepository.saveAll(entriesByLineId.values)

        val productIds = event.lines.map { it.locationProductId }
        val previousBalances = stockMovementRepository.findLatestBalances(productIds)
            .associate { it.getLocationProductId() to it.getRemainingQuantity() }

        val movements = event.lines.map { line ->
            val entry = entriesByLineId[line.deliveryLineId]!!
            val newQuantity = (previousBalances[line.locationProductId] ?: BigDecimal.ZERO) + line.quantityDelivered
            StockMovementEntity(
                stockEntryId = entry.id!!,
                locationProductId = line.locationProductId,
                movementType = MovementType.PURCHASE_RECEIVED,
                movedQuantity = line.quantityDelivered,
                remainingQuantity = newQuantity,
                externalReferenceNumber = line.lineReferenceNumber
            )
        }
        stockMovementRepository.saveAll(movements)
    }
}
