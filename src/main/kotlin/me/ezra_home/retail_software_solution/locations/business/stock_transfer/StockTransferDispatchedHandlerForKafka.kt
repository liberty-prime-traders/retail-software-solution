package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
class StockTransferDispatchedHandlerForKafka(
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val locationService: LocationService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferDispatchedEvent::class

    // sourceDocumentId = order ID (in org schema — accessible regardless of which location context runs reissue)
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderDataFetcher.getById(sourceDocumentId)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchReferenceNumber = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(order.referenceNumber).dispatch.requiredReference()
        }
        publish(
            orderId = order.id,
            orderReferenceNumber = order.referenceNumber,
            dispatchReferenceNumber = dispatchReferenceNumber,
            sourceSchema = sourceSchema,
            totalCost = Money.of(order.requiredTotalDispatchedCost()),
            postingDate = DateTimes.Local.atOrganizationZone(order.requiredDispatchedAt())
        )
    }

    fun publish(
        orderId: UUID,
        orderReferenceNumber: String,
        dispatchReferenceNumber: String,
        sourceSchema: String,
        totalCost: Money,
        postingDate: LocalDate
    ) {
        eventPublisher.publishEvent(
            StockTransferDispatchedEvent(
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
                postingDate = postingDate
            )
        )
    }
}
