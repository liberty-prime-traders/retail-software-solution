package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SupplierPaymentVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID


@Component
@TransactionalOnLocationSchema(readOnly = true)
class SupplierPaymentVoidHandlerForKafka(
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val supplierPaymentVoidRepository: SupplierPaymentVoidRepository,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SupplierPaymentVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val paymentVoid = supplierPaymentVoidRepository.getReferenceById(sourceDocumentId)
        val supplierPayment = supplierPaymentRepository.getReferenceById(paymentVoid.supplierPaymentId)
        val supplierId = purchaseDataFetcher.getSupplierId(supplierPayment.purchaseId)
        publish(paymentVoid, supplierPayment, supplierId)
    }

    fun publish(paymentVoid: SupplierPaymentVoidEntity, payment: SupplierPaymentEntity, supplierId: UUID) {
        eventPublisher.publishEvent(
            SupplierPaymentVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                voidId = paymentVoid.id!!,
                paymentId = payment.id!!,
                supplierId = supplierId,
                paymentMethodAccountCode = payment.paymentMethodAccountCode,
                amount = payment.amount,
                voidedOn = DateTimes.Local.atOrganizationZone(paymentVoid.requiredCreatedOn()),
                paymentReferenceNumber = payment.requiredReference()
            )
        )
    }
}
