package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryInventoryProcessor
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TransactionEventProducer(
  private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>,
  private val purchaseDeliveryInventoryProcessor: PurchaseDeliveryInventoryProcessor
) {

  private val log = LoggerFactory.getLogger(TransactionEventProducer::class.java)

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun onTransactionEvent(event: TransactionEvent) {
    try {
      kafkaTemplate.send(KafkaConstants.Topics.TRANSACTION_EVENTS, event.sourceContext.locationSchema, event).get()
    } catch (e: Exception) {
      log.error("Failed to publish event ${event.eventId}", e)
      when (event) {
        is PurchaseDeliveredEvent -> {
          purchaseDeliveryInventoryProcessor.markFailed(event)
        }
        else -> {
          log.warn("No specific failure handling implemented for event type ${event::class.simpleName}")
        }
      }
    }
  }
}
