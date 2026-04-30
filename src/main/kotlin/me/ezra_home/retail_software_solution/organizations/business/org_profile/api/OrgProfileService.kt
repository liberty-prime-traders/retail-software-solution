package me.ezra_home.retail_software_solution.organizations.business.org_profile.api

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.springframework.stereotype.Service

@Service
class OrgProfileService(
    private val orgDataSeeders: List<OrgDataSeeder>,
    private val asyncExecutor: AsyncExecutor
) {

    fun applySeedDefaults() {
        asyncExecutor.execute {
            ServiceAccountContext.runWithServiceAccount(ServiceAccount.RECORD_INITIALIZER) {
                orgDataSeeders.forEach { it.seed() }
            }
        }
    }
}
