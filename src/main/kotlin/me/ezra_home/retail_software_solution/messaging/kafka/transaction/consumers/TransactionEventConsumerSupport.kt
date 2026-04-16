package me.ezra_home.retail_software_solution.messaging.kafka.transaction.consumers

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSessionSetup
import me.ezra_home.retail_software_solution.messaging.kafka.notifications.ConsumerFailureEvent
import me.ezra_home.retail_software_solution.messaging.kafka.notifications.NotificationEventProducer
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.TransactionEventProcessor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class TransactionEventConsumerSupport(
    private val eventSessionSetup: EventSessionSetup,
    private val notificationProducer: NotificationEventProducer
) {
    private val logger = LoggerFactory.getLogger(TransactionEventConsumerSupport::class.java)

    fun <EVENT: TransactionEvent> consume(
        event: EVENT,
        serviceAccount: ServiceAccount,
        consumerGroup: String,
        processors: List<TransactionEventProcessor<*>>
    ) {
        @Suppress("UNCHECKED_CAST")
        val processorsForEvent = processors.filter { it.eventType == event::class } as List<TransactionEventProcessor<EVENT>>

        if (processorsForEvent.isEmpty()) {
            return
        }

        ServiceAccountContext.runWithServiceAccount(serviceAccount) {
            eventSessionSetup.initFromEvent(event)
            try {
                dispatch(event, processorsForEvent)
            } catch (e: Exception) {
                markFailed(event, processorsForEvent)
                publishNotification(event, consumerGroup, e)
            }
        }
    }

    private fun publishNotification(event: TransactionEvent, consumerGroup: String, e: Exception) {
        try {
            notificationProducer.publish(
                ConsumerFailureEvent(
                    eventId = UUID.randomUUID(),
                    sourceContext = event.sourceContext,
                    timestamp = Instant.now(),
                    correlationId = event.eventId,
                    failedEventId = event.eventId,
                    consumerGroup = consumerGroup,
                    reason = e.message ?: e.javaClass.simpleName
                )
            )
        } catch (notificationException: Exception) {
            logger.error("Failed to publish failure notification for event ${event.eventId}", notificationException)
        }
    }

    private fun <EVENT: TransactionEvent> markFailed(event: EVENT, processors: List<TransactionEventProcessor<EVENT>>) {
        try {
            processors.forEach { it.markFailed(event) }
        } catch (markFailedException: Exception) {
            logger.error("Failed to mark event ${event.eventId} as failed", markFailedException)
        }
    }


    private fun <EVENT: TransactionEvent> dispatch(event: EVENT, processors: List<TransactionEventProcessor<EVENT>>) {
        processors.filter { it.shouldProcess(event) }.forEach { it.handle(event) }
    }
}
