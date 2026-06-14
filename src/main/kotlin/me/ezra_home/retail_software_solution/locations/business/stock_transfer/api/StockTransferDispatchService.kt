package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferDispatchLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockTransferStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferFifoAllocator
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedLineDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

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
    private val locationService: LocationService
) {

    @TransactionalOnLocationSchema
    fun dispatch(orderRef: String): StockTransferResponse {
        val dispatchEntity = requireDraftDispatch(orderRef)
        val draftLines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchEntity.id!!)
        if (draftLines.isEmpty()) throw RtsGenericException("Cannot dispatch a transfer with no lines")

        val locationProductIds = draftLines.map { it.locationProductId }
        val fifoEntriesByProduct = stockBalanceFetcher.getFifoEntriesByProduct(locationProductIds)
        val productIdByLocationProductId = locationProductDataFetcher.getProductIds(locationProductIds)
        val summariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds)
        val productLabelByLocationProductId = summariesByLocationProductId.mapValues { (_, summary) -> summary.label }

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

        dispatchEntity.dispatchedById = SessionContextProvider.getUserId()
        dispatchEntity.dispatchedAt = DateTimes.Offset.Now.organization()
        dispatchEntity.status = StockTransferStatus.DISPATCHED
        stockTransferDispatchRepository.save(dispatchEntity)

        val order = stockTransferOrderService.updateStatusToDispatched(orderRef)
        stockTransferDispatchedHandlerForKafka.publish(
            orderId = order.id,
            orderRef = orderRef,
            dispatchRef = dispatchEntity.requiredReference(),
            sourceSchema = SessionContextProvider.getLocationSchema(),
            destinationSchema = locationService.getSchemaByLocationId(order.destinationLocationId),
            lines = toDispatchedLineDtos(dispatchLines)
        )
        return buildResponse(orderRef)
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

    private fun requireDraftDispatch(orderRef: String): StockTransferDispatchEntity {
        val dispatchEntity = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("Dispatch not found for order $orderRef")
        if (dispatchEntity.status != StockTransferStatus.DRAFT) {
            throw RtsGenericException("Operation only allowed in DRAFT status. Current: ${dispatchEntity.status}")
        }
        return dispatchEntity
    }

    private fun buildResponse(orderRef: String): StockTransferResponse =
        stockTransferResponseAssembler.build(stockTransferOrderService.getByReferenceNumber(orderRef))
}
