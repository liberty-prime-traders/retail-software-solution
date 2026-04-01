package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants.DEFAULT_ID

class AuthenticationSteps(private val authContext: AuthContext) {

  @Given("I am authenticated as an organization admin")
  fun authenticateAsAdmin() {
    authContext.authToken = TestConstants.Tokens.ORG_ADMIN
    authContext.currentOrganizationId = DEFAULT_ID
    authContext.currentLocationId = DEFAULT_ID
  }

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    authContext.authToken = TestConstants.Tokens.ORG_USER
    authContext.currentOrganizationId = DEFAULT_ID
    authContext.currentLocationId = DEFAULT_ID
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    authContext.authToken = null
    authContext.currentOrganizationId = DEFAULT_ID
    authContext.currentLocationId = DEFAULT_ID
  }

  @Given("I am authenticated as a platform admin")
  fun authenticateAsPlatformAdmin() {
    authContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN
    authContext.currentOrganizationId = DEFAULT_ID
    authContext.currentLocationId = DEFAULT_ID
  }
}
