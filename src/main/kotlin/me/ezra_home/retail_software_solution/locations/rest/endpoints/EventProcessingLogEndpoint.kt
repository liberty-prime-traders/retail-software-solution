package me.ezra_home.retail_software_solution.locations.rest.endpoints

import me.ezra_home.retail_software_solution.locations.business.kafka_log.api.EventRetryService
import me.ezra_home.retail_software_solution.locations.business.kafka_log.api.KafkaEventLogDto
import me.ezra_home.retail_software_solution.locations.business.kafka_log.api.KafkaEventLogsFetcher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/kafka-event-log")
class EventProcessingLogEndpoint(
    private val retryService: EventRetryService,
    private val kafkaEventLogsFetcher: KafkaEventLogsFetcher
) {

    @GetMapping
    fun getLogsForSourceId(@RequestParam("sourceDocumentId") sourceDocumentId: UUID): List<KafkaEventLogDto> {
        return kafkaEventLogsFetcher.getEventsForSourceId(sourceDocumentId)
    }

    @PostMapping("/{logId}/retry")
    fun retry(@PathVariable logId: UUID): ResponseEntity<Void> {
        retryService.retry(logId)
        return ResponseEntity.ok().build()
    }
}
