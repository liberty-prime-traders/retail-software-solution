package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityValidator
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
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
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedLineDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
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
) {

    @TransactionalOnLocationSchema
    fun dispatch(orderRef: String): StockTransferResponse {
        val dispatchEntity = stockTransferDraftDispatchFetcher.requireDraftDispatch(orderRef)
        entityAdvisoryLock.acquire(LockNamespaces.STOCK_TRANSFER_ORDER, dispatchEntity.id!!)
        val draftLines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchEntity.id!!)
        if (draftLines.isEmpty()) throw RtsGenericException("Cannot dispatch a transfer with no lines")

        val locationProductIds = draftLines.map { it.locationProductId }
        val fifoEntriesByProduct = stockBalanceFetcher.getFifoEntriesByProduct(locationProductIds)
        val productIdByLocationProductId = locationProductDataFetcher.getProductIds(locationProductIds)
        val summariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds)
        val productLabelByLocationProductId = summariesByLocationProductId.mapValues { (_, summary) -> summary.label }

        val baseQuantityNeededByLocationProductId = draftLines.associate {
            it.locationProductId to Decimals.multiplyScale4(it.quantity, it.conversionFactor)
        }
        stockAvailabilityValidator.guardSufficientStock(baseQuantityNeededByLocationProductId, productLabelByLocationProductId)

        val dispatchLines = StockTransferFifoAllocator.allocateDispatchLines(
            dispatchId = dispatchEntity.id!!,
            draftLines = draftLines,
            fifoEntriesByProduct = fifoEntriesByProduct,
            productIdByLocationProductId = productIdByLocationProductId,
            productLabelByLocationProductId = productLabelByLocationProductId
        )

        stockTransferDraftLineRepository.deleteByStockTransferDispatchId(dispatchEntity.id!!)
        stockTransferDispatchLineRepository.saveAll(dispatchLines)

        stockTransferStockUpdater.consumeStockForDispatch(toStockRequests(dispatchLines))

        val dispatchedAt = DateTimes.Offset.Now.organization()
        val dispatchedUserId = SessionContextProvider.getUserId()
        dispatchEntity.dispatchedById = dispatchedUserId
        dispatchEntity.dispatchedAt = dispatchedAt
        dispatchEntity.status = StockTransferStatus.DISPATCHED
        stockTransferDispatchRepository.save(dispatchEntity)

        val totalDispatchedCost = dispatchLines.sumOf { Decimals.multiplyScale4(it.unitCost, it.quantityDispatched) }
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
            lines = toDispatchedLineDtos(dispatchLines)
        )
        return stockTransferResponseAssembler.buildDispatchOnly(
            orderDomainDto = order,
            dispatchEntity = dispatchEntity,
            lines = toReconciledLines(dispatchLines, productLabelByLocationProductId)
        )
    }

    private fun toReconciledLines(
        dispatchLines: List<StockTransferDispatchLineEntity>,
        productLabelByLocationProductId: Map<UUID, String>
    ): List<ReconciledTransferLine> =
        dispatchLines.map { dispatchLine ->
            ReconciledTransferLine(
                dispatchLineRef = dispatchLine.requiredReference(),
                productLabel = productLabelByLocationProductId.getValue(dispatchLine.locationProductId),
                quantity = dispatchLine.quantityDispatched,
                unitId = dispatchLine.unitId,
                baseUnitId = dispatchLine.baseUnitId,
                conversionFactor = dispatchLine.conversionFactor,
                unitCost = dispatchLine.unitCost,
                quantityReceived = null
            )
        }

    private fun toStockRequests(
        dispatchLines: List<StockTransferDispatchLineEntity>
    ): List<StockTransferDispatchLineStockRequest> =
        dispatchLines.map { dispatchLine ->
            StockTransferDispatchLineStockRequest(
                dispatchLineRef = dispatchLine.requiredReference(),
                locationProductId = dispatchLine.locationProductId,
                baseQuantity = Decimals.multiplyScale4(dispatchLine.quantityDispatched, dispatchLine.conversionFactor),
                unitId = dispatchLine.unitId,
                unitCost = dispatchLine.unitCost,
                conversionFactor = dispatchLine.conversionFactor,
                baseUnitId = dispatchLine.baseUnitId
            )
        }

    private fun toDispatchedLineDtos(
        dispatchLines: List<StockTransferDispatchLineEntity>
    ): List<StockTransferDispatchedLineDto> =
        dispatchLines.map { dispatchLine ->
            StockTransferDispatchedLineDto(
                dispatchLineReferenceNumber = dispatchLine.requiredReference(),
                productId = dispatchLine.productId,
                quantityDispatched = dispatchLine.quantityDispatched,
                unitId = dispatchLine.unitId,
                baseUnitId = dispatchLine.baseUnitId,
                conversionFactor = dispatchLine.conversionFactor,
                unitCost = dispatchLine.unitCost
            )
        }
}
