package me.ezra_home.retail_software_solution.cucumber.steps.platform

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.platform.OrgAdminFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants

class AuthenticationSteps(
  private val authContext: AuthContext,
  private val orgAdminFixtureBuilder: OrgAdminFixtureBuilder,
  private val injectContext: InjectContext
) {


  @Given("I am registered as an organization admin")
  fun makeCurrentUserOrgAdmin() {
    val userId = checkNotNull(authContext.currentUserId) { "No authenticated user — call an auth step first" }
    orgAdminFixtureBuilder.register(userId)
  }

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    authContext.authToken = TestConstants.Tokens.ORG_USER
    authContext.currentUserId = TestConstants.DEFAULT_ID
    authContext.currentOrganizationId = injectContext.get(TestConstants.InjectionKeys.ORGANIZATION)
    authContext.currentLocationId = injectContext.get(TestConstants.InjectionKeys.LOCATION)
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    authContext.authToken = null
    authContext.currentUserId = null
    authContext.currentOrganizationId = injectContext.get(TestConstants.InjectionKeys.ORGANIZATION)
    authContext.currentLocationId = injectContext.get(TestConstants.InjectionKeys.LOCATION)
  }

  @Given("I am authenticated as a platform admin")
  fun authenticateAsPlatformAdmin() {
    authContext.authToken = TestConstants.Tokens.PLATFORM_ADMIN
    authContext.currentUserId = TestConstants.DEFAULT_ID
    authContext.currentOrganizationId = TestConstants.DEFAULT_ID
    authContext.currentLocationId = TestConstants.DEFAULT_ID
  }
}
