package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants.DEFAULT_ID

class AuthenticationSteps {

  @Given("I am authenticated as an organization admin")
  fun authenticateAsAdmin() {
    AuthContext.authToken = TestConstants.Tokens.ORG_ADMIN
    AuthContext.currentOrganizationId = DEFAULT_ID
    AuthContext.currentLocationId = DEFAULT_ID
  }

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    AuthContext.authToken = TestConstants.Tokens.ORG_USER
    AuthContext.currentOrganizationId = DEFAULT_ID
    AuthContext.currentLocationId = DEFAULT_ID
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    AuthContext.authToken = null
    AuthContext.currentOrganizationId = DEFAULT_ID
    AuthContext.currentLocationId = DEFAULT_ID
  }

  @Given("I am authenticated as a platform admin")
  fun authenticateAsPlatformAdmin() {
    AuthContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN
    AuthContext.currentOrganizationId = DEFAULT_ID
    AuthContext.currentLocationId = DEFAULT_ID
  }
}
