package me.ezra_home.retail_software_solution.locations.business.kafka_log.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogEntity
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogRepository
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogResolutionType
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogStatus
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.time.Instant
import java.util.UUID

@Service
class EventProcessingLogService(
    private val repository: EventProcessingLogRepository,
    private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>
) {
    private val log = LoggerFactory.getLogger(EventProcessingLogService::class.java)

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun insertPending(event: TransactionEvent, consumerGroup: String) {
        val existing = repository.findLatestByEventIdAndConsumerGroup(event.eventId, consumerGroup)
        if (existing?.status == EventProcessingLogStatus.RETRYING) return
        if (existing != null) {
            existing.status = EventProcessingLogStatus.PENDING
            existing.failedOn = null
            existing.failureReason = null
            repository.save(existing)
            return
        }
        repository.save(
            EventProcessingLogEntity(
                eventId = event.eventId,
                eventType = event::class.simpleName!!,
                consumerGroup = consumerGroup,
                sourceDocumentId = event.sourceDocumentId,
                status = EventProcessingLogStatus.PENDING
            )
        )
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markProcessed(event: TransactionEvent, consumerGroup: String) {
        val entry = repository.findLatestByEventIdAndConsumerGroup(event.eventId, consumerGroup) ?: return
        markProcessed(entry)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markProcessed(entry: EventProcessingLogEntity, resolutionType: EventProcessingLogResolutionType? = null) {
        entry.status = EventProcessingLogStatus.PROCESSED
        entry.processedOn = Instant.now()
        entry.failedOn = null
        entry.failureReason = null
        entry.resolutionType = resolutionType
        repository.save(entry)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(event: TransactionEvent, consumerGroup: String, reason: String) {
        val entry = repository.findLatestByEventIdAndConsumerGroup(event.eventId, consumerGroup) ?: return
        entry.status = EventProcessingLogStatus.FAILED
        entry.failedOn = Instant.now()
        entry.failureReason = reason
        repository.save(entry)
        publishToDlt(event, consumerGroup, entry)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markRetrying(logId: UUID) {
        val entry = repository.findById(logId)
            .orElseThrow { RtsGenericException("Event log entry $logId not found") }
        entry.status = EventProcessingLogStatus.RETRYING
        entry.resolutionType = EventProcessingLogResolutionType.DLT_REPLAY
        repository.save(entry)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun insertPublishFailed(event: TransactionEvent, reason: String) {
        repository.save(
            EventProcessingLogEntity(
                eventId = event.eventId,
                eventType = event::class.simpleName!!,
                consumerGroup = null,
                sourceDocumentId = event.sourceDocumentId,
                status = EventProcessingLogStatus.PUBLISH_FAILED,
                failedOn = Instant.now(),
                failureReason = reason
            )
        )
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun isRetrying(eventId: UUID, consumerGroup: String): Boolean {
        val existing = repository.findLatestByEventIdAndConsumerGroup(eventId, consumerGroup)
        return existing?.status == EventProcessingLogStatus.RETRYING
    }

    private fun publishToDlt(event: TransactionEvent, consumerGroup: String, entry: EventProcessingLogEntity) {
        val dltTopic = "${KafkaConstants.Topics.TRANSACTION_EVENTS}.$consumerGroup.DLT"
        try {
            val result = kafkaTemplate.send(dltTopic, event.sourceContext.locationSchema, event).get()
            entry.dltPartition = result.recordMetadata.partition()
            entry.dltOffset = result.recordMetadata.offset()
            repository.save(entry)
        } catch (e: Exception) {
            log.error("Failed to publish event ${event.eventId} to DLT $dltTopic", e)
        }
    }
}
