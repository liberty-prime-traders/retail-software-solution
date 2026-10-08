package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SupplierPaymentRecordedEvent
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SupplierPaymentHandlerForKafka(
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SupplierPaymentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val payment = supplierPaymentRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Payment $sourceDocumentId not found") }
        val context = deliveryHandlerForPurchase.getDeliveryContext(payment.purchaseId)
        publish(payment, context.supplierId)
    }

    fun publish(payment: SupplierPaymentEntity, supplierId: UUID) {
        val paymentMethodAccountCode = payment.paymentMethodAccountCode
        if (!StringUtils.hasValue(paymentMethodAccountCode)) {
            log.debug(
                "Payment method {} had no account code when payment {} was recorded — ledger entry skipped",
                payment.paymentMethodId, payment.referenceNumber
            )
            return
        }
        eventPublisher.publishEvent(
            SupplierPaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                paymentId = payment.id!!,
                supplierId = supplierId,
                paymentMethodAccountCode = paymentMethodAccountCode!!,
                amount = payment.amount,
                paymentDate = payment.paymentDate,
                paymentReferenceNumber = payment.requiredReference()
            )
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(SupplierPaymentHandlerForKafka::class.java)
    }
}
