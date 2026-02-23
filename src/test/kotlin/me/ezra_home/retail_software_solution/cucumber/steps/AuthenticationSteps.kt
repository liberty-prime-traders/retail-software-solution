package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.CucumberSpringConfiguration
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import org.springframework.beans.factory.annotation.Autowired

class AuthenticationSteps {

  @Autowired
  private lateinit var context: TestContext

  @Autowired
  private lateinit var config: CucumberSpringConfiguration

  @Given("I am authenticated as an organization admin")
  fun authenticateAsAdmin() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = "mock-admin-token"
  }

  @Given("I am authenticated as an organization user")
  fun authenticateAsUser() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = "mock-user-token"
  }

  @Given("I am not authenticated")
  fun notAuthenticated() {
    context.baseUrl = "http://localhost:${config.port}"
    context.authToken = null
  }
}
