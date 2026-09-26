package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class OpeningStockStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val stockBalanceFetcher: StockBalanceFetcher,
) {

    fun recordOpeningStock(lines: List<OpeningStockLineStockRequest>) {
        if (lines.isEmpty()) return
        val productIds = lines.map { it.locationProductId }
        val baseUnitsByProductId = locationProductDataFetcher.getBaseUnitIds(productIds)
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val runningBalances = stockBalanceFetcher.getLatestBalances(productIds).toMutableMap()

        val entriesByProductId = lines.associate { line ->
            val baseUnitId = baseUnitsByProductId.getValue(line.locationProductId)
            val baseQty = unitConversionGraph.getTarget(line.unitId, baseUnitId).applyTo(line.quantity)
            val baseCost = Decimals.divideScale4(Decimals.multiplyScale4(line.unitCost, line.quantity), baseQty)
            line.locationProductId to StockEntryEntity(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.OPENING_STOCK,
                externalReferenceNumber = line.externalReferenceNumber,
                batchSize = baseQty,
                quantityRemaining = baseQty,
                unitCost = baseCost,
                priority = 0
            )
        }
        stockEntryRepository.saveAll(entriesByProductId.values)

        val movements = lines.map { line ->
            val entry = entriesByProductId.getValue(line.locationProductId)
            val newQuantity = (runningBalances[line.locationProductId] ?: BigDecimal.ZERO) + entry.batchSize
            runningBalances[line.locationProductId] = newQuantity
            val baseUnitId = baseUnitsByProductId.getValue(line.locationProductId)
            val ratio = unitConversionGraphFacade.getRatio(line.unitId, baseUnitId)
            StockMovementEntity(
                stockEntryId = entry.id!!,
                locationProductId = line.locationProductId,
                movementType = MovementType.OPENING_STOCK,
                movedQuantity = line.quantity,
                remainingQuantity = newQuantity,
                externalReferenceNumber = line.externalReferenceNumber,
                unitId = line.unitId,
                conversionNumerator = ratio.numerator,
                conversionDenominator = ratio.denominator
            )
        }
        stockMovementRepository.saveAll(movements)
    }
}
