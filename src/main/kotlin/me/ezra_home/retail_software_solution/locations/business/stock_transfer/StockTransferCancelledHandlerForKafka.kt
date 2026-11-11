package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferCancelledEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StockTransferCancelledHandlerForKafka(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferCancelledEvent::class

    // sourceDocumentId = Transfer Order ID (org schema) — session is source when reissue is called
    @TransactionalOnLocationSchema
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderDataFetcher.getById(sourceDocumentId)
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(order.referenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order ${order.referenceNumber}")
        publish(
            orderId = order.id,
            orderReferenceNumber = order.referenceNumber,
            dispatchReferenceNumber = dispatch.requiredReference(),
            sourceSchema = SessionContextProvider.getLocationSchema(),
            totalCost = Money.of(order.requiredTotalDispatchedCost())
        )
    }

    fun publish(
        orderId: UUID,
        orderReferenceNumber: String,
        dispatchReferenceNumber: String,
        sourceSchema: String,
        totalCost: Money
    ) {
        eventPublisher.publishEvent(
            StockTransferCancelledEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = sourceSchema
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = orderId,
                orderReferenceNumber = orderReferenceNumber,
                dispatchReferenceNumber = dispatchReferenceNumber,
                totalCost = totalCost,
                postingDate = DateTimes.Local.Now.organization()
            )
        )
    }
}
