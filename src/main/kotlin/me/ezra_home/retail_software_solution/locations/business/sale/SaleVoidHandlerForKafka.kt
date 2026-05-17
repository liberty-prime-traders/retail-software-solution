package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleVoidHandlerForKafka(
    private val saleRepository: SaleRepository,
    private val saleVoidRepository: SaleVoidRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : EventReissueHandler {

    override val eventType = SaleVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val sale = saleRepository.getReferenceById(sourceDocumentId)
        val voidEntity = saleVoidRepository.findBySaleId(sourceDocumentId)
            ?: throw RtsGenericException("No sale_void record for sale $sourceDocumentId — cannot reissue SaleVoidedEvent")
        publishVoid(sale, voidEntity)
    }

    fun publishVoid(sale: SaleEntity, voidEntity: SaleVoidEntity) {
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
                saleReferenceNumber = sale.requiredReference(),
                payableTotal = sale.payableTotal(),
                dateSold = DateTimes.Local.atOrganizationZone(sale.dateSold!!),
                dateVoided = voidEntity.createdOn?.let { DateTimes.Local.atOrganizationZone(it) }
                    ?: DateTimes.Local.Now.organization()
            )
        )
    }
}
