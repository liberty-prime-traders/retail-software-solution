package me.ezra_home.retail_software_solution.locations.business.kafka_log

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.kafka_log.api.EventProcessingLogSweepService
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventProcessingLogSweeperJob(
    private val organizationService: OrganizationService,
    private val locationService: LocationService,
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
                    locationService.getAllLocationDtos().forEach { location ->
                        SessionContextProvider.initLocation(location)
                        try {
                            val ids = sweepService.findFailedToSweep(FAILED_AGE_MINUTES)
                            sweepService.retryAll(ids)
                        } catch (e: Exception) {
                            log.error("Sweep failed for org {} location {}", org.id, location.id, e)
                        }
                    }
                } finally {
                    SessionContextProvider.clear()
                }
            }
    }

    companion object {
        private const val FAILED_AGE_MINUTES = 1L
    }
}
