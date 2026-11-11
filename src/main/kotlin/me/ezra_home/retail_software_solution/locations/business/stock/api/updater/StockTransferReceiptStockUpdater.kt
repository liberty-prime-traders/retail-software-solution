package me.ezra_home.retail_software_solution.locations.business.stock.api.updater

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.stock.InboundLayerSpec
import me.ezra_home.retail_software_solution.locations.business.stock.StockIncreaseRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
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
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    // Lines that already have a TRANSFER_IN movement are skipped, so a repeated call never credits a line twice.
    // The product lock comes before that check: the check and the credit are one decision.
    fun recordTransferReceipt(
        dispatchReferenceNumber: String,
        stockTransferReceiptLineStockRequests: List<StockTransferReceiptLineStockRequest>
    ) {
        val orgProductIds = stockTransferReceiptLineStockRequests.map { it.orgProductId }.toSet()
        val identitiesByOrgProductId = locationProductDataFetcher.findIdentitiesByOrgProductIds(orgProductIds)
        val locationProductIdByOrgProductId = orgProductIds.associateWith { orgProductId ->
            identitiesByOrgProductId[orgProductId]?.locationProductId
                ?: throw RtsGenericException("No location product found for product $orgProductId at destination")
        }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIdByOrgProductId.values.toSet())

        val creditedLineReferences = stockMovementRepository.findPresentDetailReferences(
            dispatchReferenceNumber,
            stockTransferReceiptLineStockRequests.map { it.dispatchLineReferenceNumber },
            MovementType.TRANSFER_IN
        )
        val linesToCredit = stockTransferReceiptLineStockRequests
            .filter { it.dispatchLineReferenceNumber !in creditedLineReferences }
        if (linesToCredit.isEmpty()) return

        guardSameStorageUnit(linesToCredit, locationProductIdByOrgProductId)

        val layerSpecs = linesToCredit.map { line ->
            InboundLayerSpec(
                locationProductId = locationProductIdByOrgProductId.getValue(line.orgProductId),
                sourceType = StockItemSource.TRANSFER_IN,
                movementType = MovementType.TRANSFER_IN,
                documentReference = dispatchReferenceNumber,
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
        stockTransferReceiptLineStockRequests: List<StockTransferReceiptLineStockRequest>,
        locationProductIdByOrgProductId: Map<UUID, UUID>
    ) {
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val baseUnitIdByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIdByOrgProductId.values)
        stockTransferReceiptLineStockRequests.forEach { line ->
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
