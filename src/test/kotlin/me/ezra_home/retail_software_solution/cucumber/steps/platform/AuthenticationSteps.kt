package me.ezra_home.retail_software_solution.cucumber.steps.platform

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.cucumber.support.context.AuthContext

class AuthenticationSteps(private val authContext: AuthContext) {

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    authContext.authToken = TestConstants.Tokens.ORG_USER
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    authContext.authToken = null
  }
}
