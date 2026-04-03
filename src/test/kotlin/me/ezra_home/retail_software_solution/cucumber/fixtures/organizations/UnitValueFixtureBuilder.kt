package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import org.springframework.stereotype.Component

@Component
class UnitValueFixtureBuilder(
    injectContext: InjectContext,
    apiClient: ApiClient
) : FixtureBuilder<UnitValueInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/unitvalues"

  override fun defaultDto() = UnitValueInsertDto(
    name = "Test Unit",
    code = "TU",
    unitGroupId = injectContext.get(InjectionKeys.UNIT_GROUP)
  )
}
