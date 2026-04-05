package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TransactionEventProducer(
  private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>,
  private val failureHandler: TransactionEventFailureHandler
) {

  private val log = LoggerFactory.getLogger(TransactionEventProducer::class.java)

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun onTransactionEvent(event: TransactionEvent) {
    try {
      kafkaTemplate.send(KafkaConstants.Topics.TRANSACTION_EVENTS, event.sourceSchema, event).get()
    } catch (e: Exception) {
      log.error("Failed to publish event ${event.eventId}", e)
      failureHandler.handle(event)
    }
  }
}
