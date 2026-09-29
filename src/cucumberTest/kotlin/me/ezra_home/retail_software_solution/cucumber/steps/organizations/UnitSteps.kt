package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.UnitGroupFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.UnitValueFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey

class UnitSteps(
  private val unitGroupFixtureBuilder: UnitGroupFixtureBuilder,
  private val unitValueFixtureBuilder: UnitValueFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a unit group exists")
  fun createUnitGroup() {
    injectContext.store(TransientKey.UNIT_GROUP, unitGroupFixtureBuilder.create())
  }

  @Given("a unit exists")
  fun createUnit() {
    injectContext.store(TransientKey.UNIT_VALUE, unitValueFixtureBuilder.create())
  }
}
