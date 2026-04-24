package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSourceService
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphBuilder
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockItemSourceService: StockItemSourceService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphBuilder: UnitConversionGraphBuilder
) {

    fun recordPurchaseDelivery(event: PurchaseDeliveredEvent) {
        val baseUnitsByProductId = locationProductDataFetcher.getBaseUnitIds(event.lines.map { it.locationProductId })
        val sourceTypeId = stockItemSourceService.findSourceId(StockItemSource.PURCHASE)

        val entriesByLineId = event.lines.associate { line ->
            val baseUnitId = baseUnitsByProductId.getValue(line.locationProductId)
            val baseQty = unitConversionGraphBuilder.convert(line.unitId, baseUnitId, line.quantityDelivered)
            val baseCost = Decimals.divideScale4(Decimals.multiplyScale4(line.unitCost, line.quantityDelivered), baseQty)
            line.deliveryLineId to StockEntryEntity(
                purchaseDeliveryLineId = line.deliveryLineId,
                locationProductId = line.locationProductId,
                sourceTypeId = sourceTypeId,
                batchSize = baseQty,
                quantityRemaining = baseQty,
                unitCost = baseCost,
                priority = 0
            )
        }
        stockEntryRepository.saveAll(entriesByLineId.values)

        val productIds = event.lines.map { it.locationProductId }
        val previousBalances = stockMovementRepository.findLatestBalances(productIds)
            .associate { it.getLocationProductId() to it.getRemainingQuantity() }

        val movements = event.lines.map { line ->
            val entry = entriesByLineId[line.deliveryLineId]!!
            val newQuantity = (previousBalances[line.locationProductId] ?: BigDecimal.ZERO) + entry.batchSize
            val baseUnitId = baseUnitsByProductId.getValue(line.locationProductId)
            val factor = unitConversionGraphBuilder.getFactor(line.unitId, baseUnitId)
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
