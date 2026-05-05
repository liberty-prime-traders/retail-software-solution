package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleLineEventDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Decimals
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleHandlerForKafka(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SaleConfirmedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val sale = saleRepository.getReferenceById(sourceDocumentId)
        val lines = saleLineRepository.findBySaleId(sourceDocumentId)
        publish(sale, lines)
    }

    fun publish(sale: SaleEntity, lines: List<SaleLineEntity>) {
        val lineEventDtos = lines.map { SaleLineEventDto(it.locationProductId, it.quantity, it.unitPrice, it.unitId) }
        val saleTotal = lines.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
        eventPublisher.publishEvent(
            SaleConfirmedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = sale.id!!,
                contactId = sale.contactId,
                saleReferenceNumber = sale.referenceNumber!!,
                saleTotal = saleTotal,
                dateSold = DateTimes.Local.atOrganizationZone(sale.dateSold!!),
                lines = lineEventDtos
            )
        )
    }

    fun publishVoid(sale: SaleEntity, lines: List<SaleLineEntity>) {
        val lineEventDtos = lines.map { SaleLineEventDto(it.locationProductId, it.quantity, it.unitPrice, it.unitId) }
        val saleTotal = lines.sumOf { Decimals.multiplyScale4(it.quantity, it.unitPrice) }
        eventPublisher.publishEvent(
            SaleVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = sale.id!!,
                contactId = sale.contactId,
                saleReferenceNumber = sale.referenceNumber!!,
                saleTotal = saleTotal,
                dateSold = DateTimes.Local.atOrganizationZone(sale.dateSold!!),
                lines = lineEventDtos
            )
        )
    }
}
