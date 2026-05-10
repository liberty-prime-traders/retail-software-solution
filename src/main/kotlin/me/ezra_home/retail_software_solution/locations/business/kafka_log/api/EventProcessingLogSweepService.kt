package me.ezra_home.retail_software_solution.locations.business.kafka_log.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogRepository
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class EventProcessingLogSweepService(
    private val repository: EventProcessingLogRepository,
    private val retryService: EventRetryService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalOnLocationSchema(readOnly = true)
    fun findFailedToSweep(olderThanMinutes: Long): List<java.util.UUID> {
        val cutoff = OffsetDateTime.now().minus(olderThanMinutes, ChronoUnit.MINUTES)
        return repository.findByStatusAndCreatedOnBefore(EventProcessingLogStatus.FAILED, cutoff)
            .mapNotNull { it.id }
    }

    fun retryAll(logIds: List<java.util.UUID>) {
        if (logIds.isEmpty()) return
        log.info("Sweeping {} failed event log row(s)", logIds.size)
        logIds.forEach { logId ->
            try {
                retryService.retry(logId)
            } catch (e: Exception) {
                log.warn("Sweep retry of log {} failed", logId, e)
            }
        }
    }
}
