package me.ezra_home.retail_software_solution.organizations.business.kafka_log

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.kafka_log.api.EventProcessingLogSweepService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventProcessingLogSweeperJob(
    private val organizationService: OrganizationService,
    private val sweepService: EventProcessingLogSweepService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */5 * * * *")
    fun run() {
        organizationService.getAllOrganizationDtos()
            .filter { it.schemaName != null }
            .forEach { org ->
                SessionContextProvider.initOrganization(org)
                try {
                    sweepService.reclaimStalePending(
                        sweepService.findStalePending(STALE_PENDING_AGE_MINUTES)
                    )
                    sweepService.retryAll(
                        sweepService.findRetryableToSweep(RETRYABLE_AGE_MINUTES)
                    )
                } catch (e: Exception) {
                    log.error("Sweep failed for org {}", org.id, e)
                } finally {
                    SessionContextProvider.clear()
                }
            }
    }

    companion object {
        private const val RETRYABLE_AGE_MINUTES = 1L
        private const val STALE_PENDING_AGE_MINUTES = 10L
    }
}
