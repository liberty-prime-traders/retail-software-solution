package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.context.organizations.OrgContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext
import me.ezra_home.retail_software_solution.platform.business.organization.dto.OrganizationUpsertDto
import java.util.Optional
import java.util.UUID

class OrganizationCrudSteps(
  private val responseContext: ResponseContext,
  private val requestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext
) {

  @When("I get all organizations")
  fun getAllOrganizations() {
    responseContext.lastResponse = requestFactory.jsonRequest().get("/secured/organizations")
  }

  @When("I create an organization with name {string} and subdomain {string}")
  fun createOrganization(name: String, subdomain: String) {
    val organizationCreateDto = OrganizationUpsertDto(
      name = Optional.of(name),
      subdomain = subdomain
    )
    responseContext.lastResponse = requestFactory.jsonRequest()
      .body(organizationCreateDto)
      .post("/secured/organizations")
    responseContext.lastResponse?.jsonPath()?.getString("id")?.let { injectContext.store(OrgContext.ID, it) }
  }

  @When("I update the current organization with name {string}")
  fun updateOrganization(name: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .body(mapOf("name" to name))
      .put("/secured/organizations")
  }

  @When("I delete the current organization")
  fun deleteOrganization() {
    responseContext.lastResponse = requestFactory.jsonRequest().delete("/secured/organizations")
  }

  @When("I launch organization with domain {string}")
  fun launchOrganization(domain: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .post("/secured/organizations/launch/$domain")
  }

  @Given("an organization exists with id {string}")
  fun organizationExists(orgId: String) {
    AuthContext.currentOrganizationId = UUID.fromString(orgId)
  }

  @When("I get locations for organization {string}")
  fun getOrganizationLocations(orgId: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .get("/secured/organizations/$orgId/locations")
  }
}
