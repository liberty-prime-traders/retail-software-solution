package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryContext
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryHandlerForKafka(
    private val deliveryRepository: PurchaseDeliveryRepository,
    private val deliveryLineRepository: PurchaseDeliveryLineRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase
) : EventReissueHandler {

    override val eventType = PurchaseDeliveredEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val delivery = deliveryRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Delivery $sourceDocumentId not found") }
        val lines = deliveryLineRepository.findByPurchaseDeliveryIdIn(listOf(sourceDocumentId))
        val context = deliveryHandlerForPurchase.getDeliveryContext(delivery.purchaseId)
        publish(context, DeliveryRecord(delivery, lines))
    }

    fun publish(context: PurchaseDeliveryContext, deliveryRecord: DeliveryRecord) {
        val sourceContext = EventSourceContext.LocationLevel(
            orgSchema = SessionContextProvider.getOrganizationSchema(),
            locationSchema = SessionContextProvider.getLocationSchema()
        )
        eventPublisher.publishEvent(
            PurchaseDeliveryMapper.toEvent(
                context.purchaseId,
                context.supplierId,
                deliveryRecord,
                context.purchaseLineById,
                sourceContext
            )
        )
    }
}
