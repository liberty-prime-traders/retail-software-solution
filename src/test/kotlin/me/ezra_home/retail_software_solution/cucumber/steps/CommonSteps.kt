package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import org.hamcrest.Matchers.hasSize
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommonSteps {

  @Autowired
  private lateinit var context: TestContext

  @Then("the response status should be {int}")
  fun verifyStatus(expectedStatus: Int) {
    val actualStatus = context.lastResponse?.statusCode
    if (actualStatus != expectedStatus) {
      println("Response body: ${context.lastResponse?.asString()}")
    }
    assertEquals(expectedStatus, actualStatus, "Expected status $expectedStatus but got $actualStatus. Response: ${context.lastResponse?.asString()}")
  }

  @Then("the response should contain field {string}")
  fun verifyFieldExists(fieldName: String) {
    assertNotNull(context.lastResponse?.jsonPath()?.get<Any>(fieldName))
  }

  @Then("the response field {string} should be {string}")
  fun verifyFieldValue(fieldName: String, expectedValue: String) {
    assertEquals(expectedValue, context.lastResponse?.jsonPath()?.getString(fieldName))
  }

  @Then("the response should contain {int} items")
  fun verifyListSize(size: Int) {
    context.lastResponse?.then()?.body("$", hasSize<Any>(size))
  }

  @Then("the response should be an empty list")
  fun verifyEmptyList() {
    context.lastResponse?.then()?.body("$", hasSize<Any>(0))
  }
}
