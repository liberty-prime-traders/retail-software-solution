package me.ezra_home.retail_software_solution.cucumber.fixtures.locations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.util.enums.LocationType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class LocationFixtureBuilder(requestFactory: AuthenticatedRequestFactory)
  : FixtureBuilder<LocationInsertDto>(requestFactory) {

  override val endpoint = "/secured/locations"

  override fun defaultDto() = LocationInsertDto(
    locationType = LocationType.SHOP,
    name = "Location-${UUID.randomUUID().toString().take(8)}"
  )
}
