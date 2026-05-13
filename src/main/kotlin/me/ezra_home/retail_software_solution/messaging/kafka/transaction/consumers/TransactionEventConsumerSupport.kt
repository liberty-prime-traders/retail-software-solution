package me.ezra_home.retail_software_solution.messaging.kafka.transaction.consumers

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.locations.business.kafka_log.api.DltPublisher
import me.ezra_home.retail_software_solution.locations.business.kafka_log.api.EventProcessingLogService
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSessionSetup
import me.ezra_home.retail_software_solution.messaging.kafka.notifications.ConsumerFailureEvent
import me.ezra_home.retail_software_solution.messaging.kafka.notifications.NotificationEventProducer
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.processors.TransactionEventProcessor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class TransactionEventConsumerSupport(
    private val eventSessionSetup: EventSessionSetup,
    private val notificationProducer: NotificationEventProducer,
    private val logService: EventProcessingLogService,
    private val dltPublisher: DltPublisher
) {
    private val logger = LoggerFactory.getLogger(TransactionEventConsumerSupport::class.java)

    fun <EVENT: TransactionEvent> consume(
        event: EVENT,
        serviceAccount: ServiceAccount,
        consumerGroup: String,
        processors: List<TransactionEventProcessor<*>>
    ) {
        @Suppress("UNCHECKED_CAST")
        val processorsForEvent = processors.filter { it.eventType.isInstance(event) } as List<TransactionEventProcessor<EVENT>>

        if (processorsForEvent.isEmpty()) {
            return
        }

        ServiceAccountContext.runWithServiceAccount(serviceAccount) {
            eventSessionSetup.initFromEvent(event)
            try {
                dispatch(event, consumerGroup, processorsForEvent)
            } catch (e: DataIntegrityViolationException) {
                logger.info("Event ${event.eventId} lost a unique-constraint race in $consumerGroup — marking PROCESSED with RACE_LOST", e)
                markRaceLostLog(event, consumerGroup, e)
            } catch (e: Exception) {
                markFailedLog(event, consumerGroup, e)
                publishNotification(event, consumerGroup, e)
            }
        }
    }

    private fun markRaceLostLog(event: TransactionEvent, consumerGroup: String, e: Exception) {
        try {
            logService.markRaceLost(event, consumerGroup, e.message ?: e.javaClass.simpleName)
        } catch (logException: Exception) {
            logger.error("Failed to mark event ${event.eventId} as race-lost in log", logException)
        }
    }

    private fun insertPendingLog(event: TransactionEvent, consumerGroup: String) {
        try {
            logService.insertPending(event, consumerGroup)
        } catch (e: Exception) {
            logger.error("Failed to insert pending log for event ${event.eventId}", e)
        }
    }

    private fun <EVENT: TransactionEvent> dispatch(
        event: EVENT,
        consumerGroup: String,
        processors: List<TransactionEventProcessor<EVENT>>
    ) {
        val toProcess = processors.filter { processor ->
            !logService.isProcessorCompleted(event.eventId, consumerGroup, processor::class.java.simpleName)
                    && processor.shouldProcess(event)
        }
        if (toProcess.isEmpty()) {
            handleNothingToProcess(event, consumerGroup)
            return
        }
        insertPendingLog(event, consumerGroup)
        toProcess.forEach { processor ->
            processor.handle(event)
            logService.markProcessorCompleted(event.eventId, consumerGroup, processor::class.java.simpleName)
        }
        markProcessedLog(event, consumerGroup)
    }

    private fun handleNothingToProcess(event: TransactionEvent, consumerGroup: String) {
        if (logService.isRetrying(event.eventId, consumerGroup)) {
            logger.warn("Event ${event.eventId} is marked as retrying but no processors want to handle it — marking as processed to unblock")
            markProcessedLog(event, consumerGroup)
        }
    }

    private fun markProcessedLog(event: TransactionEvent, consumerGroup: String) {
        try {
            logService.markProcessed(event, consumerGroup)
        } catch (e: Exception) {
            logger.error("Failed to mark event ${event.eventId} as processed in log", e)
        }
    }

    private fun markFailedLog(event: TransactionEvent, consumerGroup: String, e: Exception) {
        val logId = try {
            logService.markFailed(event, consumerGroup, e.message ?: e.javaClass.simpleName)
        } catch (logException: Exception) {
            logger.error("Failed to mark event ${event.eventId} as failed in log", logException)
            null
        } ?: return
        try {
            dltPublisher.publish(event, consumerGroup, logId)
        } catch (dltException: Exception) {
            logger.error("Failed to dispatch event ${event.eventId} to DLT", dltException)
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
}
