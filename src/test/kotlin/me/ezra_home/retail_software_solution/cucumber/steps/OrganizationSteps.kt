package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import org.springframework.beans.factory.annotation.Autowired

class OrganizationSteps {

  @Autowired
  private lateinit var context: TestContext

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
  }

  @When("I get all organizations")
  fun getAllOrganizations() {
    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .get("/secured/organizations")
  }
}
