package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountService
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountSummaryDto
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleDiscountEventDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleLineEventDto
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleConfirmedHandlerForKafka(
    private val saleRepository: SaleRepository,
    private val saleLineRepository: SaleLineRepository,
    private val saleDiscountService: SaleDiscountService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SaleConfirmedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val sale = saleRepository.getReferenceById(sourceDocumentId)
        val lines = saleLineRepository.findBySaleId(sourceDocumentId)
        val discounts = saleDiscountService.getDiscountSummaries(sourceDocumentId)
        publish(sale, lines, discounts)
    }

    fun publish(sale: SaleEntity, lines: List<SaleLineEntity>, discounts: List<SaleDiscountSummaryDto>) {
        val lineEventDtos = lines.map { SaleLineEventDto(it.locationProductId, it.quantity, it.unitPrice, it.unitId) }
        val discountEventDtos = discounts.map { SaleDiscountEventDto(it.calculatedAmount, it.saleLineId) }
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
                subtotal = sale.subtotal!!,
                discountTotal = sale.discountTotal!!,
                dateSold = DateTimes.Local.atOrganizationZone(sale.dateSold!!),
                lines = lineEventDtos,
                discounts = discountEventDtos
            )
        )
    }
}
