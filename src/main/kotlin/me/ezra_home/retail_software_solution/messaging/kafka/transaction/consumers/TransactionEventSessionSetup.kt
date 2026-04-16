package me.ezra_home.retail_software_solution.messaging.kafka.transaction.consumers

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class TransactionEventSessionSetup(
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache
) {

    fun initializeSession(locationSchema: String) {
        // TODO implement map based lookup. May need location schemas to be unique platformwide
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
