package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferReceiptCompletedEvent
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StockTransferReceiptCompletedHandlerForKafka(
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferReceiptCompletedEvent::class

    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissue(sourceDocumentId: UUID) {
        val receipt = stockTransferReceiptRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Stock transfer receipt $sourceDocumentId not found") }
        publish(
            receiptId = receipt.id!!,
            orderRef = receipt.stockTransferOrderRef,
            receiptRef = receipt.requiredReference()
        )
    }

    fun publish(receiptId: UUID, orderRef: String, receiptRef: String) {
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
                orderReferenceNumber = orderRef,
                receiptReferenceNumber = receiptRef
            )
        )
    }
}
