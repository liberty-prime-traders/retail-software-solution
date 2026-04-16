package me.ezra_home.retail_software_solution.messaging.kafka.transaction.log

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.time.Instant

@Service
class EventProcessingLogService(
    private val repository: EventProcessingLogRepository
) {

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun insertPending(event: TransactionEvent, consumerGroup: String) {
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
        entry.status = EventProcessingLogStatus.PROCESSED
        entry.processedOn = Instant.now()
        repository.save(entry)
    }

    @TransactionalOnLocationSchema(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(event: TransactionEvent, consumerGroup: String, reason: String) {
        val entry = repository.findLatestByEventIdAndConsumerGroup(event.eventId, consumerGroup) ?: return
        entry.status = EventProcessingLogStatus.FAILED
        entry.failedOn = Instant.now()
        entry.failureReason = reason
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
}
