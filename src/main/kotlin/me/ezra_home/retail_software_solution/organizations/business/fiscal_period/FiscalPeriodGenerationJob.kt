package me.ezra_home.retail_software_solution.organizations.business.fiscal_period

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FiscalPeriodGenerationJob(
    private val organizationService: OrganizationService,
    private val generationService: FiscalPeriodGenerationService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 10 1 * * *")
    fun run() {
        organizationService.getAllOrganizationDtos()
            .filter { it.schemaName != null }
            .forEach { org ->
                SessionContextProvider.initOrganization(org)
                try {
                    ServiceAccountContext.runWithServiceAccount(ServiceAccount.RECORD_INITIALIZER) {
                        generationService.generatePeriods()
                    }
                } catch (e: Exception) {
                    log.error("Fiscal period generation failed for org ${org.id}", e)
                } finally {
                    SessionContextProvider.clear()
                }
            }
    }
}
