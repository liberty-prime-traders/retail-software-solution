package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryInventoryHandler
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.notifications.ConsumerFailureEvent
import me.ezra_home.retail_software_solution.messaging.kafka.notifications.NotificationEventProducer
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class InventoryEventConsumer(
  private val purchaseDeliveryHandler: PurchaseDeliveryInventoryHandler,
  private val contextSetup: TransactionEventContextSetup,
  private val notificationProducer: NotificationEventProducer
) {

  @KafkaListener(
    topics = [KafkaConstants.Topics.TRANSACTION_EVENTS],
    groupId = KafkaConstants.ConsumerGroups.Transaction.INVENTORY
  )
  fun onTransactionEvent(event: TransactionEvent) {
    try {
      ServiceAccountContext.runWithServiceAccount(ServiceAccount.INVENTORY_PROCESSOR) {
        contextSetup.initFromLocationSchema(event.sourceSchema)
        when (event) {
          is PurchaseDeliveredEvent -> purchaseDeliveryHandler.handle(event)
          else -> {}
        }
      }
    } catch (e: Exception) {
      notificationProducer.publish(
        ConsumerFailureEvent(
          eventId = UUID.randomUUID(),
          sourceSchema = event.sourceSchema,
          timestamp = Instant.now(),
          correlationId = event.eventId,
          failedEventId = event.eventId,
          consumerGroup = KafkaConstants.ConsumerGroups.Transaction.INVENTORY,
          reason = e.message ?: e.javaClass.simpleName
        )
      )
      throw e
    }
  }
}
