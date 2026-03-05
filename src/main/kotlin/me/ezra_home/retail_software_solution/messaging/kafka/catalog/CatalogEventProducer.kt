package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CatalogEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, CatalogChangedEvent>
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publish(event: CatalogChangedEvent) {
        kafkaTemplate.send(
            KafkaConstants.Topics.CATALOG_EVENTS,
            event.sourceSchema,
            event
        )
    }
}
