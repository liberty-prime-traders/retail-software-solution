package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.context.organizations.UnitContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitValueFixtureBuilder(requestFactory: AuthenticatedRequestFactory, injectContext: InjectContext)
  : FixtureBuilder<UnitValueInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/unitvalues"

  override fun defaultDto() = UnitValueInsertDto(
    name = "Test Unit",
    code = "TU",
    unitGroupId = UUID.fromString(injectContext.get(UnitContext.GROUP_ID))
  )
}
