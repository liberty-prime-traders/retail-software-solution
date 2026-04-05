package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
import org.springframework.stereotype.Component

@Component
class UnitGroupFixtureBuilder(
    injectContext: InjectContext,
    apiClient: ApiClient
) : FixtureBuilder<UnitGroupInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/unitgroups"

  override fun defaultDto() = UnitGroupInsertDto(name = "Test Unit Group")
}
