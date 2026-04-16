package me.ezra_home.retail_software_solution.messaging.kafka.common

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationService
import org.springframework.stereotype.Component

@Component
class EventSessionSetup(
    private val organizationService: OrganizationService,
    private val locationService: LocationService
) {

    fun initFromEvent(event: BaseEvent) {
        when (val ctx = event.sourceContext) {
            is EventSourceContext.OrgLevel -> {
                SessionContextProvider.initOrganization(organizationService.getBySchema(ctx.orgSchema))
            }
            is EventSourceContext.LocationLevel -> {
                SessionContextProvider.initOrganization(organizationService.getBySchema(ctx.orgSchema))
                SessionContextProvider.initLocation(locationService.getBySchema(ctx.locationSchema))
            }
        }
    }
}
