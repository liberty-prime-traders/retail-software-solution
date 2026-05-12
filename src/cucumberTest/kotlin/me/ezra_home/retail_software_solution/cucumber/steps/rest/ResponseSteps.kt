package me.ezra_home.retail_software_solution.cucumber.steps.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.support.assertions.PersistedResponseComparator
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.greaterThanOrEqualTo
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResponseSteps(
  private val responseContext: ResponseContext,
  private val injectContext: InjectContext,
  private val persistedResponseComparator: PersistedResponseComparator,
  private val objectMapper: ObjectMapper,
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

  @Then("the response should match json:")
  fun verifyResponseMatchesJson(expectedJson: String) {
    val responseBody = checkNotNull(responseContext.lastResponse?.asString()) {
      "Expected a response body but no response was captured"
    }
    val expectedNode = objectMapper.readTree(injectContext.inject(expectedJson))
    val actualNode = objectMapper.readTree(responseBody)
    assertJsonSubset(expectedNode, actualNode, "$")
  }

  @Then("the response should match the persisted {word}")
  fun verifyResponseMatchesPersisted(alias: String) {
    val response = checkNotNull(responseContext.lastResponse) { "Expected a response but no response was captured" }
    val actualNode = objectMapper.readTree(response.asString())
    persistedResponseComparator.assertBodyMatches(alias, actualNode, ResponseContext.idFromResponse(response))
  }

  @Then("the response item {int} should match the persisted {word} identified by {string}")
  fun verifyResponseItemMatchesPersisted(index: Int, alias: String, idReference: String) {
    val response = checkNotNull(responseContext.lastResponse) { "Expected a response but no response was captured" }
    val responseBody = response.asString()
    val actualNode = objectMapper.readTree(responseBody)
    assertTrue(actualNode.isArray, "Expected the response body to be an array but got ${actualNode.nodeType}. Response: $responseBody")
    val actualItem = actualNode.get(index)
    assertNotNull(actualItem, "Expected an item at index $index but response only had ${actualNode.size()} items")
    val persistedId = UUID.fromString(injectContext.inject(idReference))
    persistedResponseComparator.assertBodyMatches(alias, actualItem, persistedId)
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

  @Then("the response error message should be {string}")
  fun verifyErrorMessage(expectedMessage: String) {
    assertNotNull(responseContext.lastError, "Expected an error response but status was ${responseContext.lastResponse?.statusCode}")
    val actualMessage = responseContext.lastResponse?.jsonPath()?.getString("message")
    val injectedExpectedMessage = injectContext.inject(expectedMessage)
    assertEquals(
      injectedExpectedMessage,
      actualMessage,
      "Expected error message '$injectedExpectedMessage' but got '$actualMessage'"
    )
  }

  @Then("the response error field {string} should be {string}")
  fun verifyErrorField(fieldName: String, expectedValue: String) {
    assertNotNull(responseContext.lastError, "Expected an error response but status was ${responseContext.lastResponse?.statusCode}")
    assertEquals(expectedValue, responseContext.lastResponse?.jsonPath()?.getString(fieldName))
  }

  private fun assertJsonSubset(expected: JsonNode, actual: JsonNode, path: String) {
    when {
      expected.isObject -> {
        assertTrue(actual.isObject, "Expected object at $path but got ${actual.nodeType}")
        expected.fields().forEach { (fieldName, expectedChild) ->
          val actualChild = actual.get(fieldName)
          assertNotNull(actualChild, "Expected field '$fieldName' at $path but it was missing")
          assertJsonSubset(expectedChild, actualChild, "$path.$fieldName")
        }
      }

      expected.isArray -> {
        assertTrue(actual.isArray, "Expected array at $path but got ${actual.nodeType}")
        assertTrue(
          actual.size() >= expected.size(),
          "Expected at least ${expected.size()} items at $path but got ${actual.size()}"
        )
        expected.forEachIndexed { index, expectedChild ->
          assertJsonSubset(expectedChild, actual[index], "$path[$index]")
        }
      }

      else -> {
        assertEquals(expected, actual, "Mismatch at $path")
      }
    }
  }
}
