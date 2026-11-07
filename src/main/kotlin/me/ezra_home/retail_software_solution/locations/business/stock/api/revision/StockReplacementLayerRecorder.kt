package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementContexts
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockReplacementLayerRecorder(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
    private val stockEntryRepository: StockEntryRepository,
) {

    fun record(stockReplacementLayerRequest: StockReplacementLayerRequest): UUID {
        val revisedStockEntry = stockEntryRepository.findById(
            stockReplacementLayerRequest.revisedStockEntryId
        ).orElseThrow {
            RtsGenericException("Stock entry ${stockReplacementLayerRequest.revisedStockEntryId} not found.")
        }
        val layerSpec = InboundLayerSpec(
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
        return stockIncreaseRecorder.record(listOf(layerSpec)).single()
    }
}
