package me.ezra_home.retail_software_solution.messaging.kafka.catalog

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
internal class CatalogEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, CatalogChangedEvent>
) {

    fun publish(event: CatalogChangedEvent) {
        kafkaTemplate.send(
            KafkaConstants.Topics.CATALOG_EVENTS,
            event.sourceSchema,
            event
        )
    }
}
