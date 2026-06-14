package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withLocationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedLineDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StockTransferDispatchedHandlerForKafka(
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferOrderService: StockTransferOrderService,
    private val locationService: LocationService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferDispatchedEvent::class

    // sourceDocumentId = order ID (in org schema — accessible regardless of which location context runs reissue)
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderService.getById(sourceDocumentId)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val destinationSchema = locationService.getSchemaByLocationId(order.destinationLocationId)

        val dispatchRecord = withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(order.referenceNumber)
        }
        publish(
            orderId = order.id,
            orderRef = order.referenceNumber,
            dispatchRef = dispatchRecord.dispatch.requiredReference(),
            sourceSchema = sourceSchema,
            destinationSchema = destinationSchema,
            lines = dispatchRecord.lines.map { line ->
                StockTransferDispatchedLineDto(
                    dispatchLineReferenceNumber = line.requiredReference(),
                    productId = line.productId,
                    quantityDispatched = line.quantityDispatched,
                    unitId = line.unitId,
                    baseUnitId = line.baseUnitId,
                    conversionFactor = line.conversionFactor,
                    unitCost = line.unitCost
                )
            }
        )
    }

    fun publish(
        orderId: UUID,
        orderRef: String,
        dispatchRef: String,
        sourceSchema: String,
        destinationSchema: String,
        lines: List<StockTransferDispatchedLineDto>
    ) {
        eventPublisher.publishEvent(
            StockTransferDispatchedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = destinationSchema
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = orderId,
                orderReferenceNumber = orderRef,
                dispatchReferenceNumber = dispatchRef,
                sourceLocationSchema = sourceSchema,
                destinationLocationSchema = destinationSchema,
                lines = lines
            )
        )
    }
}
