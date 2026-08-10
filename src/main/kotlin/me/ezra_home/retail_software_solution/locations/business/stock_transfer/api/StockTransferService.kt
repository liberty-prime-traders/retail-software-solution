package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferCancelledHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferSchemaGateway
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockTransferService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val stockTransferCancelledHandlerForKafka: StockTransferCancelledHandlerForKafka,
    private val locationService: LocationService
) {

    fun createTransfer(stockTransferCreateDto: StockTransferCreateDto): StockTransferResponse {
        val order = stockTransferOrderService.createOrder(
            sourceLocationId = SessionContextProvider.getLocationId(),
            destinationLocationId = stockTransferCreateDto.destinationLocationId,
            notes = stockTransferCreateDto.notes
        )
        val dispatchEntity = stockTransferDispatchRepository.save(StockTransferDispatchEntity(order.referenceNumber))
        return stockTransferResponseAssembler.buildDispatchOnly(order, dispatchEntity)
    }

    fun cancelTransfer(orderRef: String): StockTransferResponse {
        val order = stockTransferOrderDataFetcher.getByReferenceNumber(orderRef)
        if (order.status != StockTransferStatus.DRAFT && order.status != StockTransferStatus.DISPATCHED) {
            throw RtsGenericException("Cannot cancel a transfer in status ${order.status}")
        }
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val wasDispatched = order.status == StockTransferStatus.DISPATCHED
        val dispatchRef = locationService.withLocationSchema(sourceSchema) { stockTransferSchemaGateway.cancelDispatch(orderRef) }
        stockTransferOrderService.updateStatusToCancelled(orderRef)
        if (wasDispatched) {
            stockTransferCancelledHandlerForKafka.publish(
                orderId = order.id,
                orderRef = orderRef,
                dispatchRef = dispatchRef,
                sourceSchema = sourceSchema
            )
        }
        return stockTransferResponseAssembler.build(stockTransferOrderDataFetcher.getByReferenceNumber(orderRef))
    }

    fun fetchTop(limit: Int): List<StockTransferResponse> {
        return stockTransferOrderDataFetcher.getTopByLocationId(limit)
            .map { order -> stockTransferResponseAssembler.build(order) }
    }

    fun fetchByOrderRef(orderRef: String): StockTransferResponse {
        val order = stockTransferOrderDataFetcher.getByReferenceNumber(orderRef)
        return stockTransferResponseAssembler.build(order)
    }
}
