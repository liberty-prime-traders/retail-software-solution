package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.CucumberSpringConfiguration
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class AuthenticationSteps {

  companion object {
    private val DEFAULT_ORG_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val DEFAULT_LOCATION_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
  }

  @Autowired
  private lateinit var context: TestContext

  @Autowired
  private lateinit var config: CucumberSpringConfiguration

  @Given("I am authenticated as an organization admin")
  fun authenticateAsAdmin() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = "mock-org-admin-token"
    context.currentOrganizationId = DEFAULT_ORG_ID
    context.currentLocationId = DEFAULT_LOCATION_ID
  }

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = "mock-user-token"
    context.currentOrganizationId = DEFAULT_ORG_ID
    context.currentLocationId = DEFAULT_LOCATION_ID
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = null
    context.currentOrganizationId = DEFAULT_ORG_ID
    context.currentLocationId = DEFAULT_LOCATION_ID
  }

  @Given("I am authenticated as a platform admin")
  fun authenticateAsPlatformAdmin() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = "mock-platform-admin-token"
    context.currentOrganizationId = DEFAULT_ORG_ID
    context.currentLocationId = DEFAULT_LOCATION_ID
  }
}
