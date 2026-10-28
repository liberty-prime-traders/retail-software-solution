package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferDispatchLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.ReconciledTransferLine
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftDispatchFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferFifoAllocator
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferSchemaGateway
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.toDispatchedLineDto
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StockTransferDispatchService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val stockTransferStockUpdater: StockTransferStockUpdater,
    private val stockTransferDispatchedHandlerForKafka: StockTransferDispatchedHandlerForKafka,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val locationService: LocationService,
    private val userQualifier: UserQualifier,
    private val stockTransferDraftDispatchFetcher: StockTransferDraftDispatchFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val stockAvailabilityValidator: StockAvailabilityValidator,
    private val stockBreakdownProvider: StockBreakdownProvider,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    @TransactionalOnLocationSchema
    fun dispatch(orderRef: String): StockTransferResponse {
        val dispatchEntity = stockTransferDraftDispatchFetcher.requireDraftDispatch(orderRef)
        entityAdvisoryLock.acquire(LockNamespaces.STOCK_TRANSFER_ORDER, dispatchEntity.id!!)
        val draftLines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchEntity.id!!)
        if (draftLines.isEmpty()) throw RtsGenericException("Cannot dispatch a transfer with no lines")

        val locationProductIds = draftLines.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByProduct = stockBalanceFetcher.getFifoEntriesByProduct(locationProductIds)
        val orgProductIdByLocationProductId = locationProductDataFetcher.getOrgProductIds(locationProductIds)
        val summariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds)
        val productLabelByLocationProductId = summariesByLocationProductId.mapValues { (_, summary) -> summary.label }

        val storageQuantityNeededByLocationProductId = draftLines.associate {
            it.locationProductId to it.storageQuantity()
        }
        stockAvailabilityValidator.guardSufficientStock(storageQuantityNeededByLocationProductId, productLabelByLocationProductId)
        guardDestinationCountsInSameStorageUnit(
            stockTransferOrderDataFetcher.getByReferenceNumber(orderRef).destinationLocationId,
            draftLines,
            summariesByLocationProductId,
            orgProductIdByLocationProductId
        )

        val dispatchLines = StockTransferFifoAllocator.allocateDispatchLines(
            dispatchId = dispatchEntity.id!!,
            draftLines = draftLines,
            fifoEntriesByProduct = fifoEntriesByProduct,
            orgProductIdByLocationProductId = orgProductIdByLocationProductId,
            quantityBreakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(locationProductIds.toSet()),
            productLabelByLocationProductId = productLabelByLocationProductId
        )

        stockTransferDraftLineRepository.deleteByStockTransferDispatchId(dispatchEntity.id!!)
        stockTransferDispatchLineRepository.saveAll(dispatchLines)

        stockTransferStockUpdater.consumeStockForDispatch(toStockRequests(dispatchEntity.requiredReference(), dispatchLines))

        val dispatchedAt = DateTimes.Offset.Now.organization()
        val dispatchedUserId = SessionContextProvider.getUserId()
        dispatchEntity.dispatchedById = dispatchedUserId
        dispatchEntity.dispatchedAt = dispatchedAt
        dispatchEntity.status = StockTransferStatus.DISPATCHED
        stockTransferDispatchRepository.save(dispatchEntity)

        val totalDispatchedCost = dispatchLines.sumOf { it.costDispatched }
        stockTransferOrderService.setDispatchSummary(
            referenceNumber = orderRef,
            lineCount = dispatchLines.size,
            totalDispatchedCost = totalDispatchedCost,
            dispatchedAt = dispatchedAt,
            dispatchedByName = userQualifier.getUserFullName(dispatchedUserId)
        )

        val order = stockTransferOrderService.updateStatusToDispatched(orderRef)
        stockTransferDispatchedHandlerForKafka.publish(
            orderId = order.id,
            orderRef = orderRef,
            dispatchRef = dispatchEntity.requiredReference(),
            sourceSchema = SessionContextProvider.getLocationSchema(),
            destinationSchema = locationService.getSchemaByLocationId(order.destinationLocationId),
            lines = dispatchLines.map { it.toDispatchedLineDto() }
        )
        return stockTransferResponseAssembler.buildDispatchOnly(
            orderDomainDto = order,
            dispatchEntity = dispatchEntity,
            lines = toReconciledLines(dispatchLines, summariesByLocationProductId)
        )
    }

    private fun toReconciledLines(
        dispatchLines: List<StockTransferDispatchLineEntity>,
        summariesByLocationProductId: Map<UUID, LocationProductSummaryDto>
    ): List<ReconciledTransferLine> {
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(summariesByLocationProductId.keys)
        return dispatchLines.map { dispatchLine ->
            val summary = summariesByLocationProductId.getValue(dispatchLine.locationProductId)
            ReconciledTransferLine(
                dispatchLineRef = dispatchLine.requiredReference(),
                productLabel = summary.label,
                quantity = dispatchLine.wholeLineUserProvidedQuantity,
                unitId = dispatchLine.unitId,
                baseUnitId = summary.baseUnitId,
                conversionFactor = BaseUnitDisplay.conversionFactor(
                    dispatchLine.storageUnitsPerUserProvidedUnit, unitConversionGraph.getSizeInRoot(summary.baseUnitId)
                ),
                unitCost = dispatchLine.unitCost,
                totalCost = dispatchLine.costDispatched,
                quantityReceived = null,
                storageQuantity = dispatchLine.storageQuantityDispatched,
                storageBreakdownText = breakdownByLocationProductId.getValue(dispatchLine.locationProductId)
                    .render(ExactQuantity.ofStorage(dispatchLine.storageQuantityDispatched))
            )
        }
    }

    private fun toStockRequests(
        dispatchReference: String,
        dispatchLines: List<StockTransferDispatchLineEntity>
    ): List<StockTransferDispatchLineStockRequest> =
        dispatchLines.map { dispatchLine ->
            StockTransferDispatchLineStockRequest(
                dispatchReference = dispatchReference,
                dispatchLineReference = dispatchLine.requiredReference(),
                locationProductId = dispatchLine.locationProductId,
                storageQuantity = ExactQuantity.ofStorage(dispatchLine.storageQuantityDispatched),
                userProvidedQuantity = dispatchLine.wholeLineUserProvidedQuantity,
                unitId = dispatchLine.unitId,
                storageUnitsPerUserProvidedUnit = dispatchLine.storageUnitsPerUserProvidedUnit,
            )
        }

    // The dispatched quantity is in the source's storage unit, so the destination must count stock in the same root; checked before any stock event
    private fun guardDestinationCountsInSameStorageUnit(
        destinationLocationId: UUID,
        draftLines: List<StockTransferDraftLineEntity>,
        summariesByLocationProductId: Map<UUID, LocationProductSummaryDto>,
        orgProductIdByLocationProductId: Map<UUID, UUID>
    ) {
        val destinationSchema = locationService.getSchemaByLocationId(destinationLocationId)
        val destinationBaseUnitIdByOrgProductId = locationService.withLocationSchema(destinationSchema) {
            stockTransferSchemaGateway.readBaseUnitIdsByOrgProductId(orgProductIdByLocationProductId.values)
        }
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        draftLines.forEach { draftLine ->
            val sourceStorageUnitId = unitConversionGraph.getStorageUnitId(summariesByLocationProductId.getValue(draftLine.locationProductId).baseUnitId)
            val orgProductId = orgProductIdByLocationProductId.getValue(draftLine.locationProductId)
            val destinationBaseUnitId = destinationBaseUnitIdByOrgProductId[orgProductId]
                ?: throw RtsGenericException("The destination location does not stock product $orgProductId")
            if (unitConversionGraph.getStorageUnitId(destinationBaseUnitId) != sourceStorageUnitId) {
                throw RtsGenericException(SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER)
            }
        }
    }

    companion object {
        const val SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER =
            "The source and destination products count stock in different storage units, so the transfer cannot be dispatched"
    }
}
