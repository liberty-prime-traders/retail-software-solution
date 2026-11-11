package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferReceiptCompletedEvent
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StockTransferReceiptCompletedHandlerForKafka(
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val locationService: LocationService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferReceiptCompletedEvent::class

    // sourceDocumentId = receipt ID (destination schema) — session is the destination when reissue is called
    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissue(sourceDocumentId: UUID) {
        val receipt = stockTransferReceiptRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Stock transfer receipt $sourceDocumentId not found") }
        val order = stockTransferOrderDataFetcher.getByReferenceNumber(receipt.stockTransferOrderRef)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchReferenceNumber = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(order.referenceNumber).dispatch.requiredReference()
        }
        publish(
            receiptId = receipt.id!!,
            orderReferenceNumber = receipt.stockTransferOrderRef,
            dispatchReferenceNumber = dispatchReferenceNumber,
            receiptReferenceNumber = receipt.requiredReference(),
            totalCost = Money.of(order.requiredTotalDispatchedCost())
        )
    }

    fun publish(
        receiptId: UUID,
        orderReferenceNumber: String,
        dispatchReferenceNumber: String,
        receiptReferenceNumber: String,
        totalCost: Money
    ) {
        eventPublisher.publishEvent(
            StockTransferReceiptCompletedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = receiptId,
                orderReferenceNumber = orderReferenceNumber,
                dispatchReferenceNumber = dispatchReferenceNumber,
                totalCost = totalCost,
                receiptReferenceNumber = receiptReferenceNumber,
                postingDate = DateTimes.Local.Now.organization()
            )
        )
    }
}
