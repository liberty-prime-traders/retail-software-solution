package me.ezra_home.retail_software_solution.cucumber.support.initialization

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.model.LocationEntity
import me.ezra_home.retail_software_solution.platform.model.OrganizationEntity
import org.springframework.stereotype.Component

@Component
class BoilerPlateSessionContextHolder {

  var org: OrganizationEntity? = null
  var location: LocationEntity? = null

  fun requireOrg(): OrganizationEntity = org ?: error("Boilerplate org not initialized")
  fun requireLocation(): LocationEntity = location ?: error("Boilerplate location not initialized")

  fun withOrgSession(block: () -> Unit) {
    val snapshot = SessionContextProvider.getSession().copy()
    try {
      SessionContextProvider.initOrganization(requireOrg())
      block()
    } finally {
      SessionContextProvider.setSession(snapshot)
    }
  }
}
