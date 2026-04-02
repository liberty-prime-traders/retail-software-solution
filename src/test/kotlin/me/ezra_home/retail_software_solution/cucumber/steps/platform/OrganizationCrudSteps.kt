package me.ezra_home.retail_software_solution.cucumber.steps.platform

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.getResponseId
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationInsertDto
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpdateDto
import java.util.Optional
import java.util.UUID

class OrganizationCrudSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val authContext: AuthContext
) {

  @When("I get all organizations")
  fun getAllOrganizations() {
    apiClient.get("/secured/organizations")
  }

  @When("I create an organization with name {string} and subdomain {string}, and passcode {string}")
  fun createOrganization(name: String, subdomain: String, passcode: String) {
    val response = apiClient.post(
      "/secured/organizations",
      OrganizationInsertDto(name = name, subdomain = subdomain, passCode = UUID.fromString(passcode))
    )
    injectContext.store(InjectionKeys.ORGANIZATION, response.getResponseId())
  }

  @When("I update the current organization with name {string}")
  fun updateOrganization(name: String) {
    apiClient.put("/secured/organizations", OrganizationUpdateDto(name = Optional.of(name)))
  }

  @When("I delete the current organization")
  fun deleteOrganization() {
    apiClient.delete("/secured/organizations")
  }

  @When("I launch organization with domain {string}")
  fun launchOrganization(domain: String) {
    apiClient.post("/secured/organizations/launch/$domain")
  }

  @Given("an organization exists with id {string}")
  fun organizationExists(orgId: String) {
    authContext.currentOrganizationId = UUID.fromString(orgId)
  }

  @When("I get locations for organization {string}")
  fun getOrganizationLocations(orgId: String) {
    apiClient.get("/secured/organizations/$orgId/locations")
  }
}
