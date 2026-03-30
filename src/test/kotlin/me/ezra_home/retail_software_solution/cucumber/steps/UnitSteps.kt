package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.organizations.UnitContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.getResponseId
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto.UnitGroupInsertDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueInsertDto
import java.util.UUID
import kotlin.test.assertEquals

class UnitSteps(
  private val requestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext
) {

  @Given("a unit group exists")
  fun createUnitGroup() {
    val response = requestFactory.jsonRequest()
      .body(UnitGroupInsertDto(name = "Test Unit Group"))
      .post("/secured/unitgroups")
    assertEquals(200, response.statusCode, "Failed to create unit group. Response: ${response.asString()}")
    injectContext.store(UnitContext.GROUP_ID, response.getResponseId())
  }

  @Given("a unit exists")
  fun createUnit() {
    val groupId = injectContext.find(UnitContext.GROUP_ID) ?: run {
      createUnitGroup()
      injectContext.get(UnitContext.GROUP_ID)
    }

    val response = requestFactory.jsonRequest()
      .body(UnitValueInsertDto(name = "Test Unit", code = "TU", unitGroupId = UUID.fromString(groupId)))
      .post("/secured/unitvalues")
    assertEquals(200, response.statusCode, "Failed to create unit value. Response: ${response.asString()}")
    injectContext.store(UnitContext.VALUE_ID, response.getResponseId())
  }
}
