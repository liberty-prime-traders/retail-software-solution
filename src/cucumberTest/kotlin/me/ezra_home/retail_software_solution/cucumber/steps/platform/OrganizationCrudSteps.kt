package me.ezra_home.retail_software_solution.cucumber.steps.platform

import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.platform.business.organization.api.OrganizationUpdateDto
import java.util.Optional

class OrganizationCrudSteps(
  private val apiClient: ApiClient
) {

  @When("I get all organizations")
  fun getAllOrganizations() {
    apiClient.get("/secured/organizations")
  }

  @When("I update the current organization with name {string}")
  fun updateOrganization(name: String) {
    apiClient.put("/secured/organizations", OrganizationUpdateDto(name = Optional.of(name)))
  }

  @When("I launch organization with domain {string}")
  fun launchOrganization(domain: String) {
    apiClient.post("/secured/organizations/launch/$domain")
  }

  @When("I get locations for organization {string}")
  fun getOrganizationLocations(orgId: String) {
    apiClient.get("/secured/organizations/$orgId/locations")
  }
}
