package me.ezra_home.retail_software_solution.locations.business.kafka_log.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class KafkaEventLogsFetcher(private val eventProcessingLogRepository: EventProcessingLogRepository) {

    fun getEventsForSourceId(sourceId: UUID): List<KafkaEventLogDto> {
        return eventProcessingLogRepository.findBySourceDocumentId(sourceId).map {
            KafkaEventLogDto(
                id = it.id!!,
                sourceDocumentId = it.sourceDocumentId,
                referenceNumber = it.requiredReference(),
                processor = it.consumerGroup,
                status = it.status,
                processedOn = it.processedOn,
                failureReason = it.failureReason
            )
        }
    }
}
