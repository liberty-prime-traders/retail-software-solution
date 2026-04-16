package me.ezra_home.retail_software_solution.messaging.kafka.transaction.consumers

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.location.LocationCache
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class TransactionEventSessionSetup(
    private val organizationCache: OrganizationCache,
    private val locationCache: LocationCache
) {

    fun initializeSession(context: EventSourceContext.LocationLevel) {
        val org = organizationCache.getAllOrganizations().find { it.schemaName == context.orgSchema }
            ?: throw RtsGenericException("No organization found for schema ${context.orgSchema}.")
        val location = locationCache.getAllLocations().find { it.schemaName == context.locationSchema }
            ?: throw RtsGenericException("No location found for schema ${context.locationSchema}.")
        SessionContextProvider.initOrganization(org)
        SessionContextProvider.initLocation(location)
    }
}
