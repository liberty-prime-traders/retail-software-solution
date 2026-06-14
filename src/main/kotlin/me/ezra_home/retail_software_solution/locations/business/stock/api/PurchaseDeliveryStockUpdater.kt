package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val stockBalanceFetcher: StockBalanceFetcher,
) {

    fun recordPurchaseDelivery(event: PurchaseDeliveredEvent) {
        val baseUnitsByProductId = locationProductDataFetcher.getBaseUnitIds(event.lines.map { it.locationProductId })
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()

        val entriesByLineId = event.lines.associate { line ->
            val baseUnitId = baseUnitsByProductId.getValue(line.locationProductId)
            val baseQty = unitConversionGraph.getTarget(line.unitId, baseUnitId).applyTo(line.quantityDelivered)
            val baseCost = Decimals.divideScale4(Decimals.multiplyScale4(line.unitCost, line.quantityDelivered), baseQty)
            line.deliveryLineId to StockEntryEntity(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.PURCHASE,
                externalReferenceNumber = line.lineReferenceNumber,
                batchSize = baseQty,
                quantityRemaining = baseQty,
                unitCost = baseCost,
                priority = 0
            )
        }
        stockEntryRepository.saveAll(entriesByLineId.values)

        val productIds = event.lines.map { it.locationProductId }
        val previousBalances = stockBalanceFetcher.getLatestBalances(productIds)

        val movements = event.lines.map { line ->
            val entry = entriesByLineId[line.deliveryLineId]!!
            val newQuantity = (previousBalances[line.locationProductId] ?: BigDecimal.ZERO) + entry.batchSize
            val baseUnitId = baseUnitsByProductId.getValue(line.locationProductId)
            val factor = unitConversionGraphFacade.getFactor(line.unitId, baseUnitId)
            StockMovementEntity(
                stockEntryId = entry.id!!,
                locationProductId = line.locationProductId,
                movementType = MovementType.PURCHASE_RECEIVED,
                movedQuantity = line.quantityDelivered,
                remainingQuantity = newQuantity,
                externalReferenceNumber = line.lineReferenceNumber,
                unitId = line.unitId,
                conversionFactor = factor
            )
        }
        stockMovementRepository.saveAll(movements)
    }
}
