package me.ezra_home.retail_software_solution.locations.business.kafka_log.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogEntity
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogRepository
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogResolutionType
import me.ezra_home.retail_software_solution.messaging.kafka.common.DltReader
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventRetryService(
    private val repository: EventProcessingLogRepository,
    private val logService: EventProcessingLogService,
    private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>,
    private val dltReader: DltReader<TransactionEvent>,
    private val reissueHandlers: List<EventReissueHandler>
) {
    private val log = LoggerFactory.getLogger(EventRetryService::class.java)

    @TransactionalOnLocationSchema
    fun retry(logId: UUID) {
        val entry = repository.findById(logId).orElseThrow {
            RtsGenericException("Event log entry $logId not found")
        }

        val dltRecord = if (entry.consumerGroup != null) findOnDlt(entry) else null
        if (dltRecord != null) {
            kafkaTemplate.send(KafkaConstants.Topics.TRANSACTION_EVENTS, dltRecord.key(), dltRecord.value()).get()
            logService.markRetrying(logId)
            return
        }

        val handler = reissueHandlers.find { it.eventType.simpleName == entry.eventType }
        if (handler == null) {
            log.error("No reissue handler for event type '${entry.eventType}' — manual intervention required for log entry $logId")
            throw RtsGenericException("No reissue handler registered for event type '${entry.eventType}'")
        }
        handler.reissue(entry.sourceDocumentId)
        logService.markProcessed(entry, EventProcessingLogResolutionType.REISSUED)
    }

    private fun findOnDlt(entry: EventProcessingLogEntity): ConsumerRecord<String, TransactionEvent>? {
        val partition = entry.dltPartition ?: return null
        val offset = entry.dltOffset ?: return null
        val dltTopic = KafkaConstants.Topics.transactionDlt(entry.consumerGroup!!)
        return try {
            dltReader.fetchAt(dltTopic, partition, offset)
        } catch (e: Exception) {
            log.warn("DLT fetch failed for event ${entry.eventId} at $dltTopic[$partition]@$offset, will attempt reissue", e)
            null
        }
    }
}
