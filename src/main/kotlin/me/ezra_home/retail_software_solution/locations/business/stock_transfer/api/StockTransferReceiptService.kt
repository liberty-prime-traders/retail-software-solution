package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.lock.api.LockNamespaces
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
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockTransferReceiptService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferReceiptLineRepository: StockTransferReceiptLineRepository,
    private val stockTransferReceiptCompletedHandlerForKafka: StockTransferReceiptCompletedHandlerForKafka,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val locationService: LocationService
) {

    fun confirmLine(orderRef: String, dispatchLineRef: String): StockTransferResponse {
        val order = stockTransferOrderService.getByReferenceNumber(orderRef)
        if (order.status != StockTransferStatus.DISPATCHED) {
            throw RtsGenericException("Can only confirm lines on a DISPATCHED transfer. Current: ${order.status}")
        }

        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchLine = withLocationSchema(sourceSchema) { stockTransferSchemaGateway.readDispatchLine(dispatchLineRef) }
        val destinationLocationProductId = locationProductDataFetcher
            .findIdentityByProductId(dispatchLine.productId).locationProductId

        entityAdvisoryLock.acquire(LockNamespaces.STOCK_TRANSFER_ORDER, order.id)

        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderRef)
            ?: stockTransferReceiptRepository.saveAndFlush(
                StockTransferReceiptEntity(
                    stockTransferOrderRef = orderRef,
                    receivedById = SessionContextProvider.getUserId(),
                    receivedAt = DateTimes.Offset.Now.organization()
                )
            )

        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw RtsGenericException("Cannot confirm lines on a completed receipt")
        }

        if (!stockTransferReceiptLineRepository.existsByStockTransferDispatchLineRef(dispatchLineRef)) {
            stockTransferReceiptLineRepository.save(
                StockTransferReceiptLineEntity(
                    stockTransferReceiptId = receipt.id!!,
                    stockTransferDispatchLineRef = dispatchLineRef,
                    locationProductId = destinationLocationProductId,
                    quantityReceived = dispatchLine.quantityDispatched
                )
            )
        }

        return buildResponse(order)
    }

    fun unconfirmLine(orderRef: String, dispatchLineRef: String): StockTransferResponse {
        val order = stockTransferOrderService.getByReferenceNumber(orderRef)
        entityAdvisoryLock.acquire(LockNamespaces.STOCK_TRANSFER_ORDER, order.id)
        val receipt = stockTransferReceiptRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("No receipt found for order $orderRef")

        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw RtsGenericException("Cannot unconfirm lines on a completed receipt")
        }

        val receiptLine = stockTransferReceiptLineRepository.findByStockTransferDispatchLineRef(dispatchLineRef)
            ?: throw RtsGenericException("Receipt line for dispatch line $dispatchLineRef not found")

        stockTransferReceiptLineRepository.delete(receiptLine)
        return buildResponse(order)
    }

    fun completeReceipt(receiptRef: String): StockTransferResponse {
        val receipt = stockTransferReceiptRepository.findByReferenceNumber(receiptRef)
            ?: throw RtsGenericException("Receipt $receiptRef not found")
        if (receipt.status != StockTransferReceiptStatus.PENDING) {
            throw RtsGenericException("Receipt is already ${receipt.status}")
        }

        val orderRef = receipt.stockTransferOrderRef
        val order = stockTransferOrderService.getByReferenceNumber(orderRef)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)

        val allDispatchLines = withLocationSchema(sourceSchema) { stockTransferSchemaGateway.readAllDispatchLines(orderRef) }
        val receiptLines = stockTransferReceiptLineRepository.findByStockTransferReceiptId(receipt.id!!)
        val confirmedRefs = receiptLines.map { it.stockTransferDispatchLineRef }.toSet()
        val unconfirmedCount = allDispatchLines.count { it.requiredReference() !in confirmedRefs }
        if (unconfirmedCount > 0) {
            throw RtsGenericException("Cannot complete receipt: $unconfirmedCount dispatch line(s) not yet confirmed")
        }

        receipt.status = StockTransferReceiptStatus.COMPLETED
        stockTransferReceiptRepository.save(receipt)

        withLocationSchema(sourceSchema) { stockTransferSchemaGateway.markDispatchCompleted(orderRef) }
        stockTransferOrderService.updateStatusToCompleted(orderRef)

        stockTransferReceiptCompletedHandlerForKafka.publish(
            receiptId = receipt.id!!,
            orderRef = orderRef,
            receiptRef = receipt.requiredReference()
        )

        return buildResponse(order)
    }

    private fun buildResponse(order: StockTransferOrderDomainDto): StockTransferResponse =
        stockTransferResponseAssembler.build(order)
}
