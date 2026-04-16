package me.ezra_home.retail_software_solution.messaging.kafka.notifications

import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class NotificationEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ConsumerFailureEvent>
) {

    private val log = LoggerFactory.getLogger(NotificationEventProducer::class.java)

    fun publish(event: ConsumerFailureEvent) {
        try {
            kafkaTemplate.send(KafkaConstants.Topics.NOTIFICATIONS, event.sourceContext.orgSchema, event)
        } catch (e: Exception) {
            log.error("Failed to publish notification for event ${event.failedEventId}", e)
        }
    }
}
