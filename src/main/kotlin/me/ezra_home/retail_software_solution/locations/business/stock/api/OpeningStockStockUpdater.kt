package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockInboundLayers
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class OpeningStockStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider,
) {

    fun recordOpeningStock(lines: List<OpeningStockLineStockRequest>) {
        if (lines.isEmpty()) return
        val productIds = lines.map { it.locationProductId }
        val runningBalances = stockBalanceFetcher.getLatestBalances(productIds).toMutableMap()
        val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(productIds)

        val layerSpecs = lines.map { line ->
            InboundLayerSpec(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.OPENING_STOCK,
                movementType = MovementType.OPENING_STOCK,
                documentReference = line.documentReference,
                detailReference = null,
                storageQuantity = ExactQuantity.ofUserProvided(line.userProvidedQuantity, line.storageUnitsPerUnit),
                batchCost = Decimals.MoneyScale.multiply(line.unitCost, line.userProvidedQuantity),
                userProvidedQuantity = line.userProvidedQuantity,
                unitId = line.unitId,
                storageUnitsPerUnit = line.storageUnitsPerUnit,
            )
        }
        val entries = stockEntryRepository.saveAll(layerSpecs.map { StockInboundLayers.newLayer(it) })

        val movements = layerSpecs.zip(entries).map { (layerSpec, entry) ->
            val newBalance = ExactQuantity.ofStorage(runningBalances[layerSpec.locationProductId] ?: BigDecimal.ZERO) + layerSpec.storageQuantity
            runningBalances[layerSpec.locationProductId] = newBalance.toStorageQuantity()
            StockInboundLayers.inboundMovement(layerSpec, entry, newBalance, breakdownByLocationProductId.getValue(layerSpec.locationProductId))
        }
        stockMovementRepository.saveAll(movements)
    }
}
