package me.ezra_home.retail_software_solution.cucumber.steps.rest

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.greaterThanOrEqualTo
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResponseSteps(
  private val responseContext: ResponseContext,
  private val injectContext: InjectContext
) {

  @Then("the response status should be {int}")
  fun verifyStatus(expectedStatus: Int) {
    val actualStatus = responseContext.lastResponse?.statusCode
    assertEquals(
      expectedStatus, actualStatus,
      "Expected status $expectedStatus but got $actualStatus. Response: ${responseContext.lastResponse?.asString()}"
    )
  }

  @Then("the response should contain field {string}")
  fun verifyFieldExists(fieldName: String) {
    assertNotNull(responseContext.lastResponse?.jsonPath()?.get<Any>(fieldName))
  }

  @Then("the response field {string} should be {string}")
  fun verifyFieldValue(fieldName: String, expectedValue: String) {
    assertEquals(expectedValue, responseContext.lastResponse?.jsonPath()?.getString(fieldName))
  }

  @Then("the response should contain:")
  fun verifyFields(table: DataTable) {
    table.asMaps().forEach { row ->
      val field = row["field"]!!
      val expected = injectContext.inject(row["value"]!!)
      assertEquals(
        expected,
        responseContext.lastResponse?.jsonPath()?.getString(field),
        "Response field '$field' mismatch"
      )
    }
  }

  @Then("the response should contain {int} items")
  fun verifyListSize(size: Int) {
    responseContext.lastResponse?.then()?.body("$", hasSize<Any>(size))
  }

  @Then("the response should contain at least {int} items")
  fun verifyListHasAtLeastSize(size: Int) {
    responseContext.lastResponse?.then()?.body("size()", greaterThanOrEqualTo(size))
  }

  @Then("the response should be an empty list")
  fun verifyEmptyList() {
    responseContext.lastResponse?.then()?.body("$", hasSize<Any>(0))
  }

  @Then("the response error should contain {string}")
  fun verifyErrorContains(expectedMessage: String) {
    val error = responseContext.lastError
    assertNotNull(error, "Expected an error response but status was ${responseContext.lastResponse?.statusCode}")
    assertTrue(error.contains(expectedMessage, ignoreCase = true), "Expected error containing '$expectedMessage' but got: $error")
  }

  @Then("the response error field {string} should be {string}")
  fun verifyErrorField(fieldName: String, expectedValue: String) {
    assertNotNull(responseContext.lastError, "Expected an error response but status was ${responseContext.lastResponse?.statusCode}")
    assertEquals(expectedValue, responseContext.lastResponse?.jsonPath()?.getString(fieldName))
  }

}
