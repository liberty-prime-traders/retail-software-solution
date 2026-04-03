package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey
import me.ezra_home.retail_software_solution.cucumber.support.initialization.BoilerPlateSessionContextHolder
import me.ezra_home.retail_software_solution.organizations.business.location.LocationService

class LocationSteps(
  private val holder: BoilerPlateSessionContextHolder,
  private val locationService: LocationService,
  private val injectContext: InjectContext
) {

  @Given("a location context exists")
  fun restoreLocation() {
    holder.withOrgSession {
      holder.location = locationService.reinsertLocation(holder.requireLocation())
      injectContext.store(TransientKey.LOCATION, holder.requireLocation().getNullSafeId())
    }
  }
}
