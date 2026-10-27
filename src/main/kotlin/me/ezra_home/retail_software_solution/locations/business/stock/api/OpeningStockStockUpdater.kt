package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class OpeningStockStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
) {

    fun recordOpeningStock(lines: List<OpeningStockLineStockRequest>) {
        val layerSpecs = lines.map { line ->
            InboundLayerSpec(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.OPENING_STOCK,
                movementType = MovementType.OPENING_STOCK,
                documentReference = line.documentReference,
                detailReference = null,
                storageQuantity = ExactQuantity.ofUserProvided(line.userProvidedQuantity, line.storageUnitsPerUserProvidedUnit),
                batchCost = Decimals.MoneyScale.multiply(line.unitCost, line.userProvidedQuantity.toBigDecimal()),
                userProvidedQuantity = line.userProvidedQuantity.toBigDecimal(),
                unitId = line.unitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
            )
        }
        stockIncreaseRecorder.record(layerSpecs)
    }
}
