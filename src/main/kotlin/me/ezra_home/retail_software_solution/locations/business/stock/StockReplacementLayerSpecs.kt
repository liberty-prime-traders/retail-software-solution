package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRequest
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity

object StockReplacementLayerSpecs {

    fun of(
        stockReplacementLayerRequest: StockReplacementLayerRequest,
        revisedStockEntry: StockEntryEntity
    ): InboundLayerSpec = InboundLayerSpec(
        locationProductId = stockReplacementLayerRequest.locationProductId,
        sourceType = stockReplacementLayerRequest.sourceType,
        movementType = stockReplacementLayerRequest.movementType,
        documentReference = stockReplacementLayerRequest.documentReference,
        detailReference = stockReplacementLayerRequest.detailReference,
        storageQuantity = ExactQuantity.ofStorageFromUserProvided(
            stockReplacementLayerRequest.userProvidedQuantity,
            stockReplacementLayerRequest.storageUnitsPerUserProvidedUnit
        ),
        batchCost = stockReplacementLayerRequest.batchCost,
        userProvidedQuantity = stockReplacementLayerRequest.userProvidedQuantity,
        unitId = stockReplacementLayerRequest.unitId,
        storageUnitsPerUserProvidedUnit = stockReplacementLayerRequest.storageUnitsPerUserProvidedUnit,
        context = StockMovementContexts.replacementFromStockRevision(
            stockReplacementLayerRequest.stockRevisionReferenceNumber
        ),
        activatedOn = revisedStockEntry.requiredActivatedOn(),
    )
}
