package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class OrganizationCrudSteps {

  @Autowired
  private lateinit var context: TestContext

  @Autowired
  private lateinit var dataManager: TestDataManager

  @When("I get all organizations")
  fun getAllOrganizations() {
    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .get("/secured/organizations")
  }

  @When("I create an organization with name {string} and subdomain {string}")
  fun createOrganization(name: String, subdomain: String) {
    val orgData = mapOf(
      "name" to name,
      "subdomain" to subdomain
    )

    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .body(orgData)
      .post("/secured/organizations")

    if (context.lastResponse?.statusCode == 201) {
      val orgId = context.lastResponse?.jsonPath()?.getString("id")
      orgId?.let {
        dataManager.track("organization", UUID.fromString(it))
        context.currentOrganizationId = UUID.fromString(it)
      }
    }
  }

  @When("I update the current organization with name {string}")
  fun updateOrganization(name: String) {
    val orgData = mapOf(
      "name" to name
    )

    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .body(orgData)
      .put("/secured/organizations")
  }

  @When("I delete the current organization")
  fun deleteOrganization() {
    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .delete("/secured/organizations")
  }

  @When("I launch organization with domain {string}")
  fun launchOrganization(domain: String) {
    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .post("/secured/organizations/launch/$domain")
  }

  @Given("an organization exists with id {string}")
  fun organizationExists(orgId: String) {
    context.currentOrganizationId = UUID.fromString(orgId)
  }

  @When("I get locations for organization {string}")
  fun getOrganizationLocations(orgId: String) {
    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .get("/secured/organizations/$orgId/locations")
  }
}
