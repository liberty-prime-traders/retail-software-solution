package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentRecordedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentHandlerForKafka(
    private val saleDataFetcher: SaleDataFetcher,
    private val salePaymentRepository: SalePaymentRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SalePaymentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) = publishExistingForSale(sourceDocumentId)

    fun publishExistingForSale(saleId: UUID) {
        val contactId = saleDataFetcher.getSaleContactId(saleId)
        val payments = salePaymentRepository.findBySaleId(saleId)
        if (payments.isEmpty()) return
        publishEvent(saleId, contactId, payments.map { toLine(it) })
    }

    fun publish(saleId: UUID, contactId: UUID, payments: List<SalePaymentEntity>) {
        if (payments.isNotEmpty()) {
            publishEvent(saleId, contactId, payments.map { toLine(it) })
        }
    }

    private fun toLine(payment: SalePaymentEntity) = SalePaymentLineDto(
        paymentReferenceNumber = payment.requiredReference(),
        paymentMethodAccountCode = payment.paymentMethodAccountCode,
        amount = payment.amount,
        paymentDate = payment.paymentDate
    )

    private fun publishEvent(saleId: UUID, contactId: UUID, payments: List<SalePaymentLineDto>) {
        eventPublisher.publishEvent(
            SalePaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = saleId,
                contactId = contactId,
                payments = payments
            )
        )
    }
}
