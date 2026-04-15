package me.ezra_home.retail_software_solution.messaging.kafka.transaction

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class TransactionEventContextSetup(
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache
) {

    fun initFromLocationSchema(locationSchema: String) {
        for (org in organizationCache.getAllOrganizations()) {
            SessionContextProvider.initOrganization(org)
            val location = locationCache.getAllLocations().find { it.schemaName == locationSchema }
            if (location != null) {
                SessionContextProvider.initLocation(location)
                return
            }
        }
        throw RtsGenericException("No location found for schema $locationSchema.")
    }
}
