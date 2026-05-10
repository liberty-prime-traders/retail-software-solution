package me.ezra_home.retail_software_solution.locations.business.kafka_log.api

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withSession
import me.ezra_home.retail_software_solution.messaging.kafka.common.KafkaConstants
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.TransactionEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DltPublisher(
    private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>,
    private val logService: EventProcessingLogService
) {
    private val log = LoggerFactory.getLogger(DltPublisher::class.java)

    fun publish(event: TransactionEvent, consumerGroup: String, logId: UUID) {
        val dltTopic = KafkaConstants.Topics.transactionDlt(consumerGroup)
        // Capture session here; the callback runs on a Kafka producer thread without one.
        val session = SessionContextProvider.getSession().copy()
        kafkaTemplate.send(dltTopic, event.sourceContext.locationSchema, event)
            .whenComplete { result, throwable ->
                if (throwable == null) {
                    runCatching {
                        withSession(session) {
                            logService.recordDltPublished(
                                logId,
                                result.recordMetadata.partition(),
                                result.recordMetadata.offset()
                            )
                        }
                    }.onFailure {
                        log.error("Failed to record DLT publish result for log $logId, event ${event.eventId}", it)
                    }
                } else {
                    log.error("Failed to publish event ${event.eventId} to DLT $dltTopic", throwable)
                    runCatching {
                        withSession(session) {
                            logService.markDltPublishFailed(
                                logId,
                                throwable.message ?: throwable.javaClass.simpleName
                            )
                        }
                    }.onFailure {
                        log.error("Failed to mark log $logId as DLT_PUBLISH_FAILED for event ${event.eventId}", it)
                    }
                }
            }
    }
}
