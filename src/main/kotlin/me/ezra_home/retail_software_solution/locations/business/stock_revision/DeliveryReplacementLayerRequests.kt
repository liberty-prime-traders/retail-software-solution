package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRequest
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

object DeliveryReplacementLayerRequests {

    fun of(
        locationProductId: UUID,
        deliveryReferenceNumber: String,
        replacementLineReferenceNumber: String,
        stockRevisionReferenceNumber: String,
        userProvidedQuantityDelivered: BigDecimal,
        unitId: UUID,
        storageUnitsPerUserProvidedUnit: Long,
        unitCost: BigDecimal,
        revisedStockEntryId: UUID,
    ): StockReplacementLayerRequest = StockReplacementLayerRequest(
        locationProductId = locationProductId,
        sourceType = StockItemSource.PURCHASE,
        movementType = MovementType.PURCHASE_RECEIVED,
        documentReference = deliveryReferenceNumber,
        detailReference = replacementLineReferenceNumber,
        stockRevisionReferenceNumber = stockRevisionReferenceNumber,
        userProvidedQuantity = userProvidedQuantityDelivered,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        batchCost = Decimals.MoneyScale.multiply(unitCost, userProvidedQuantityDelivered),
        revisedStockEntryId = revisedStockEntryId,
    )
}
