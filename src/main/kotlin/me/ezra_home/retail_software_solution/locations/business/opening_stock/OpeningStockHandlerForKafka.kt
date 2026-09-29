package me.ezra_home.retail_software_solution.locations.business.opening_stock

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.OpeningStockDeclaredEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class OpeningStockHandlerForKafka(
    private val openingStockRepository: OpeningStockRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = OpeningStockDeclaredEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val row = openingStockRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Opening stock $sourceDocumentId not found") }
        publish(row)
    }

    fun publish(openingStock: OpeningStockEntity) {
        eventPublisher.publishEvent(
            OpeningStockDeclaredEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                openingStockId = requireNotNull(openingStock.id),
                ledgerSourceReferenceNumber = openingStock.requiredReference(),
                locationProductId = openingStock.locationProductId,
                quantity = openingStock.quantity,
                unitCost = openingStock.unitCost,
                postingDate = DateTimes.Local.atOrganizationZone(openingStock.requiredCreatedOn())
            )
        )
    }
}
