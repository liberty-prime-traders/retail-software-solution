package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupInsertDto
import org.springframework.stereotype.Component

@Component
class UnitGroupFixtureBuilder(requestFactory: AuthenticatedRequestFactory, injectContext: InjectContext)
  : FixtureBuilder<UnitGroupInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/unitgroups"

  override fun defaultDto() = UnitGroupInsertDto(name = "Test Unit Group")
}
