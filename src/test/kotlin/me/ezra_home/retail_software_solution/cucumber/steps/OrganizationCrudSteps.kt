package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext
import java.util.UUID

class OrganizationCrudSteps(
  private val authContext: AuthContext,
  private val responseContext: ResponseContext,
  private val requestFactory: AuthenticatedRequestFactory
) {

  @When("I get all organizations")
  fun getAllOrganizations() {
    responseContext.lastResponse = requestFactory.jsonRequest().get("/secured/organizations")
  }

  @When("I create an organization with name {string} and subdomain {string}")
  fun createOrganization(name: String, subdomain: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .body(mapOf("name" to name, "subdomain" to subdomain))
      .post("/secured/organizations")

    if (responseContext.lastResponse?.statusCode == 201) {
      val orgId = responseContext.lastResponse?.jsonPath()?.getString("id")
      orgId?.let {
        authContext.currentOrganizationId = UUID.fromString(it)
      }
    }
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
    authContext.currentOrganizationId = UUID.fromString(orgId)
  }

  @When("I get locations for organization {string}")
  fun getOrganizationLocations(orgId: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .get("/secured/organizations/$orgId/locations")
  }
}
