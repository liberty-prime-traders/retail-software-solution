package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.updater.StockTransferReceiptStockUpdater
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.toReceiptLineStockRequest
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferOrderLocker
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptCompletedHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptStatus
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferReceiptRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferSchemaGateway
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockTransferReceiptService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferReceiptLineRepository: StockTransferReceiptLineRepository,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockTransferReceiptCompletedHandlerForKafka: StockTransferReceiptCompletedHandlerForKafka,
    private val stockTransferReceiptStockUpdater: StockTransferReceiptStockUpdater,
    private val stockTransferOrderLocker: StockTransferOrderLocker,
    private val locationService: LocationService
) {

    fun confirmLine(orderReferenceNumber: String, dispatchLineReferenceNumber: String): StockTransferResponse {
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        if (order.status != StockTransferStatus.DISPATCHED) {
            throw RtsGenericException("Can only confirm lines on a DISPATCHED transfer. Current: ${order.status}")
        }

        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchLine = locationService.withLocationSchema(sourceSchema) { stockTransferSchemaGateway.readDispatchLine(dispatchLineReferenceNumber) }
        val destinationLocationProductId = locationProductDataFetcher
            .findIdentityByOrgProductId(dispatchLine.orgProductId).locationProductId

        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: stockTransferReceiptRepository.save(
                StockTransferReceiptEntity(
                    stockTransferOrderRef = orderReferenceNumber,
                    receivedById = SessionContextProvider.getUserId(),
                    receivedAt = DateTimes.Offset.Now.organization()
                )
            )

        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw RtsGenericException("Cannot confirm lines on a completed receipt")
        }

        if (!stockTransferReceiptLineRepository.existsByStockTransferDispatchLineRef(dispatchLineReferenceNumber)) {
            stockTransferReceiptLineRepository.save(
                StockTransferReceiptLineEntity(
                    stockTransferReceiptId = receipt.id!!,
                    stockTransferDispatchLineRef = dispatchLineReferenceNumber,
                    locationProductId = destinationLocationProductId,
                    storageQuantityReceived = dispatchLine.storageQuantityDispatched
                )
            )
        }

        return buildReceiptResponse(order, receipt)
    }

    fun unconfirmLine(orderReferenceNumber: String, dispatchLineReferenceNumber: String): StockTransferResponse {
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderReferenceNumber)
            ?: throw RtsGenericException("No receipt found for order $orderReferenceNumber")

        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw RtsGenericException("Cannot unconfirm lines on a completed receipt")
        }

        val receiptLine = stockTransferReceiptLineRepository
            .findByStockTransferDispatchLineRef(dispatchLineReferenceNumber)
            ?: throw RtsGenericException("Receipt line for dispatch line $dispatchLineReferenceNumber not found")

        stockTransferReceiptLineRepository.delete(receiptLine)
        return buildReceiptResponse(order, receipt)
    }

    fun completeTransfer(receiptReferenceNumber: String): StockTransferResponse {
        val orderReferenceNumber = stockTransferReceiptRepository
            .findStockTransferOrderRefByReferenceNumber(receiptReferenceNumber)
            ?: throw RtsGenericException("Receipt $receiptReferenceNumber not found")
        val order = stockTransferOrderLocker.lockThenFetch(orderReferenceNumber)
        // A COMPLETED order with a PENDING receipt is a completion whose credit never committed: the order and the dispatch
        // commit before the credit does. The credit is idempotent, so running it again finishes the transfer.
        if (order.status != StockTransferStatus.DISPATCHED && order.status != StockTransferStatus.COMPLETED) {
            throw RtsGenericException("Can only complete a DISPATCHED transfer. Current: ${order.status}")
        }
        val receipt = stockTransferReceiptRepository.findByReferenceNumber(receiptReferenceNumber)
            ?: throw RtsGenericException("Receipt $receiptReferenceNumber not found")
        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw RtsGenericException("Receipt is already ${receipt.status}")
        }
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)

        val dispatchWithLines = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(orderReferenceNumber)
        }
        if (dispatchWithLines.dispatch.status == StockTransferStatus.CANCELLED) {
            throw RtsGenericException(
                "The dispatch of transfer $orderReferenceNumber was cancelled, so it cannot be completed"
            )
        }
        val allDispatchLines = dispatchWithLines.lines
        val receiptLines = stockTransferReceiptLineRepository.findByStockTransferReceiptId(receipt.id!!)
        val confirmedDispatchLineReferenceNumbers = receiptLines.map { it.stockTransferDispatchLineRef }.toSet()
        val unconfirmedCount = allDispatchLines.count {
            it.requiredReference() !in confirmedDispatchLineReferenceNumbers
        }
        if (unconfirmedCount > 0) {
            throw RtsGenericException("Cannot complete transfer #$orderReferenceNumber: $unconfirmedCount dispatch line(s) not yet confirmed")
        }

        stockTransferReceiptStockUpdater.recordTransferReceipt(
            dispatchReferenceNumber = dispatchWithLines.dispatch.requiredReference(),
            stockTransferReceiptLineStockRequests = allDispatchLines.map { it.toReceiptLineStockRequest() }
        )

        receipt.status = StockTransferReceiptStatus.COMPLETED
        stockTransferReceiptRepository.save(receipt)

        locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.markDispatchCompleted(orderReferenceNumber)
        }
        stockTransferOrderService.updateStatusToCompleted(orderReferenceNumber)

        stockTransferReceiptCompletedHandlerForKafka.publish(
            receiptId = receipt.id!!,
            orderReferenceNumber = orderReferenceNumber,
            dispatchReferenceNumber = dispatchWithLines.dispatch.requiredReference(),
            receiptReferenceNumber = receipt.requiredReference(),
            totalCost = Money.of(order.requiredTotalDispatchedCost())
        )

        return buildReceiptResponse(stockTransferOrderDataFetcher.getByReferenceNumber(orderReferenceNumber), receipt)
    }

    private fun buildReceiptResponse(
        order: StockTransferOrderDomainDto,
        receipt: StockTransferReceiptEntity
    ): StockTransferResponse {
        val receiptLines = stockTransferReceiptLineRepository.findByStockTransferReceiptId(receipt.id!!)
        return stockTransferResponseAssembler.buildDispatchAndReceipt(order, receipt, receiptLines)
    }
}
