package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
) {

    fun recordPurchaseDelivery(event: PurchaseDeliveredEvent) {
        val layerSpecs = event.lines.map { line ->
            InboundLayerSpec(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.PURCHASE,
                movementType = MovementType.PURCHASE_RECEIVED,
                documentReference = event.deliveryReferenceNumber,
                detailReference = line.lineReferenceNumber,
                storageQuantity = ExactQuantity.ofStorageFromUserProvided(line.userProvidedQuantityDelivered, line.storageUnitsPerUserProvidedUnit),
                batchCost = Decimals.MoneyScale.multiply(line.unitCost, line.userProvidedQuantityDelivered),
                userProvidedQuantity = line.userProvidedQuantityDelivered,
                unitId = line.unitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
            )
        }
        stockIncreaseRecorder.record(layerSpecs)
    }
}
