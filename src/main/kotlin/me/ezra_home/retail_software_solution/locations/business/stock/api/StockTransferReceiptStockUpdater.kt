package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockTransferReceiptStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun findUnprocessedLineRefs(dispatchReference: String, lineRefs: Collection<String>): Set<String> {
        if (lineRefs.isEmpty()) return emptySet()
        val processed = stockMovementRepository.findPresentDetailReferences(dispatchReference, lineRefs, MovementType.TRANSFER_IN)
        return lineRefs.toSet() - processed
    }

    fun recordTransferReceipt(event: StockTransferDispatchedEvent) {
        val orgProductIds = event.lines.map { it.orgProductId }.toSet()
        val identitiesByOrgProductId = locationProductDataFetcher.findIdentitiesByOrgProductIds(orgProductIds)
        val locationProductIdByOrgProductId = orgProductIds.associateWith { orgProductId ->
            identitiesByOrgProductId[orgProductId]?.locationProductId
                ?: throw RtsGenericException("No location product found for product $orgProductId at destination")
        }

        guardSameStorageUnit(event, locationProductIdByOrgProductId)

        val layerSpecs = event.lines.map { line ->
            InboundLayerSpec(
                locationProductId = locationProductIdByOrgProductId.getValue(line.orgProductId),
                sourceType = StockItemSource.TRANSFER_IN,
                movementType = MovementType.TRANSFER_IN,
                documentReference = event.dispatchReferenceNumber,
                detailReference = line.dispatchLineReferenceNumber,
                storageQuantity = ExactQuantity.ofStorage(line.storageQuantityDispatched),
                batchCost = line.costDispatched,
                userProvidedQuantity = line.wholeLineUserProvidedQuantity,
                unitId = line.unitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
            )
        }
        stockIncreaseRecorder.record(layerSpecs)
    }

    // The dispatch quantity is in the source's storage unit, so the destination must count stock in the same root
    private fun guardSameStorageUnit(
        event: StockTransferDispatchedEvent,
        locationProductIdByOrgProductId: Map<UUID, UUID>
    ) {
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val baseUnitIdByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIdByOrgProductId.values)
        event.lines.forEach { line ->
            val destinationBaseUnitId = baseUnitIdByLocationProductId.getValue(locationProductIdByOrgProductId.getValue(line.orgProductId))
            if (unitConversionGraph.getStorageUnitId(line.unitId) != unitConversionGraph.getStorageUnitId(destinationBaseUnitId)) {
                throw RtsGenericException(SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER)
            }
        }
    }

    companion object {
        const val SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER =
            "The source and destination products count stock in different storage units, so the transfer cannot be received"
    }
}
