package me.ezra_home.retail_software_solution.locations.business.kafka_log.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogRepository
import me.ezra_home.retail_software_solution.locations.business.kafka_log.EventProcessingLogStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class EventProcessingLogSweepService(
    private val repository: EventProcessingLogRepository,
    private val retryService: EventRetryService,
    private val logService: EventProcessingLogService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalOnLocationSchema(readOnly = true)
    fun findRetryableToSweep(olderThanMinutes: Long): List<UUID> {
        val cutoff = OffsetDateTime.now().minus(olderThanMinutes, ChronoUnit.MINUTES)
        return repository.findByStatusInAndCreatedOnBefore(RETRYABLE_STATUSES, cutoff)
            .mapNotNull { it.id }
    }

    @TransactionalOnLocationSchema(readOnly = true)
    fun findStalePending(olderThanMinutes: Long): List<UUID> {
        val cutoff = OffsetDateTime.now().minus(olderThanMinutes, ChronoUnit.MINUTES)
        return repository.findByStatusInAndCreatedOnBefore(listOf(EventProcessingLogStatus.PENDING), cutoff)
            .mapNotNull { it.id }
    }

    fun reclaimStalePending(logIds: List<UUID>) {
        if (logIds.isEmpty()) return
        log.info("Reclaiming {} stale PENDING event log row(s) → FAILED", logIds.size)
        logIds.forEach { logId ->
            try {
                logService.markPendingTimedOut(logId)
            } catch (e: Exception) {
                log.warn("Reclaim of stale PENDING log {} failed", logId, e)
            }
        }
    }

    fun retryAll(logIds: List<UUID>) {
        if (logIds.isEmpty()) return
        log.info("Sweeping {} retryable event log row(s)", logIds.size)
        logIds.forEach { logId ->
            try {
                retryService.retry(logId)
            } catch (e: Exception) {
                log.warn("Sweep retry of log {} failed", logId, e)
            }
        }
    }

    companion object {
        private val RETRYABLE_STATUSES = listOf(
            EventProcessingLogStatus.FAILED,
            EventProcessingLogStatus.DLT_PUBLISH_FAILED
        )
    }
}
